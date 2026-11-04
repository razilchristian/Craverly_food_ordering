package com.cravely.backend.service;

import com.cravely.backend.config.RazorpayProperties;
import com.cravely.backend.dto.PaymentFailureRequest;
import com.cravely.backend.dto.PaymentResponse;
import com.cravely.backend.dto.RazorpayOrderResponse;
import com.cravely.backend.dto.VerifyPaymentRequest;
import com.cravely.backend.entity.*;
import com.cravely.backend.exception.ResourceNotFoundException;
import com.cravely.backend.repository.OrderRepository;
import com.cravely.backend.repository.PaymentRepository;
import com.cravely.backend.repository.UserRepository;
import com.cravely.backend.repository.WebhookEventRepository;
import org.json.JSONObject;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final RazorpayClientWrapper razorpayClientWrapper;
    private final RazorpayProperties razorpayProperties;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          UserRepository userRepository,
                          WebhookEventRepository webhookEventRepository,
                          RazorpayClientWrapper razorpayClientWrapper,
                          RazorpayProperties razorpayProperties) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.razorpayClientWrapper = razorpayClientWrapper;
        this.razorpayProperties = razorpayProperties;
    }

    // ==========================================
    // 1. CREATE RAZORPAY ORDER (INITIATE PAYMENT)
    // ==========================================

    @Transactional
    public RazorpayOrderResponse createRazorpayOrder(String customerEmail, Long orderId) {
        User customer = getCustomer(customerEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        validateOrderOwnership(order, customer);
        validateOrderEligibilityForPayment(order);

        // Calculate amount in paise (1 INR = 100 paise) using BigDecimal
        BigDecimal totalAmount = order.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Order total amount must be greater than zero");
        }

        long amountInPaise = totalAmount.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValue();

        String currency = razorpayProperties.getCurrency() != null ? razorpayProperties.getCurrency() : "INR";

        // Call Razorpay API to generate order
        var rzpOrder = razorpayClientWrapper.createOrder(amountInPaise, currency, order.getOrderNumber());

        // Save Payment attempt in MySQL (status remains PENDING until captured)
        Payment payment = new Payment(order, rzpOrder.id(), totalAmount, currency);
        paymentRepository.save(payment);

        String keyId = razorpayProperties.getKeyId();
        if (keyId == null || keyId.trim().isEmpty()) {
            keyId = "rzp_test_unconfigured";
        }

        return new RazorpayOrderResponse(
                rzpOrder.id(),
                keyId,
                amountInPaise,
                totalAmount,
                currency,
                order.getId(),
                order.getOrderNumber(),
                customer.getFullName(),
                customer.getEmail(),
                order.getCustomerPhone()
        );
    }

    // ==========================================
    // 2. VERIFY PAYMENT SIGNATURE & CONFIRMATION
    // ==========================================

    @Transactional
    public PaymentResponse verifyPayment(String customerEmail, VerifyPaymentRequest request) {
        User customer = getCustomer(customerEmail);

        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment attempt not found for Razorpay order ID: " + request.getRazorpayOrderId()));

        Order order = payment.getOrder();
        validateOrderOwnership(order, customer);

        // Idempotency: if already marked PAID, return existing verified response safely
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            return PaymentResponse.fromEntity(payment);
        }

        // If genuine payment verification is attempted without Razorpay credentials configured
        boolean isMockOrder = request.getRazorpayOrderId() != null && request.getRazorpayOrderId().startsWith("order_mock_");
        if (!isMockOrder && !razorpayProperties.isConfigured()) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Razorpay payment gateway is not configured on the server. Genuine payments cannot be verified.");
            paymentRepository.save(payment);
            throw new IllegalStateException("Razorpay payment gateway is not configured on the server. Genuine payments cannot be verified.");
        }

        // Verify cryptographic HMAC-SHA256 signature
        boolean isValidSignature = razorpayClientWrapper.verifyPaymentSignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isValidSignature) {
            payment.setFailureReason("Invalid Razorpay payment signature");
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new IllegalArgumentException("Invalid payment signature");
        }

        // Authoritative verification with Razorpay API
        var paymentDetails = razorpayClientWrapper.fetchPayment(request.getRazorpayPaymentId());
        String status = paymentDetails.status();

        // Verify amount and currency consistency if provided by Razorpay
        long expectedPaise = payment.getAmount().multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValue();

        if (paymentDetails.amount() != null && !paymentDetails.amount().equals(expectedPaise)) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment amount mismatch: expected " + expectedPaise + " paise but received " + paymentDetails.amount() + " paise");
            paymentRepository.save(payment);
            throw new IllegalStateException("Payment amount mismatch: expected " + expectedPaise + " but received " + paymentDetails.amount());
        }

        if (paymentDetails.currency() != null && !payment.getCurrency().equalsIgnoreCase(paymentDetails.currency())) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment currency mismatch: expected " + payment.getCurrency() + " but received " + paymentDetails.currency());
            paymentRepository.save(payment);
            throw new IllegalStateException("Payment currency mismatch");
        }

        // Phase 2 Rule: Only "captured" payments become PAID. "authorized" is NOT captured!
        if ("captured".equalsIgnoreCase(status)) {
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setRazorpaySignature(request.getRazorpaySignature());
            payment.setPaymentStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setPaymentMethod(paymentDetails.method());

            // If order was cancelled or rejected before verification, flag for reconciliation/refund
            if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.REJECTED) {
                payment.setFailureReason("REQUIRES_REFUND: Payment captured after order was " + order.getOrderStatus() + ". Flagged for manual reconciliation/refund.");
            }

            Payment savedPayment = paymentRepository.save(payment);

            // Update order's payment status to PAID, preserving the operational status (never reactivate)
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);

            return PaymentResponse.fromEntity(savedPayment);
        } else if ("authorized".equalsIgnoreCase(status)) {
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setPaymentMethod(paymentDetails.method());
            payment.setFailureReason("Payment is authorized but not captured");
            paymentRepository.save(payment);
            throw new IllegalStateException("Payment is authorized but not yet captured. Capture confirmation is required before completion.");
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Razorpay payment status is not captured: " + status);
            paymentRepository.save(payment);
            throw new IllegalStateException("Payment status is not captured: " + status);
        }
    }

    // ==========================================
    // 2B. RECORD PAYMENT FAILURE (CLIENT CALLBACK)
    // ==========================================

    @Transactional
    public PaymentResponse recordPaymentFailure(String customerEmail, PaymentFailureRequest request) {
        User customer = getCustomer(customerEmail);

        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment attempt not found for Razorpay order ID: " + request.getRazorpayOrderId()));

        Order order = payment.getOrder();
        validateOrderOwnership(order, customer);

        // Never downgrade an already PAID payment
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            return PaymentResponse.fromEntity(payment);
        }

        if (request.getRazorpayPaymentId() != null && !request.getRazorpayPaymentId().trim().isEmpty()) {
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        }

        payment.setPaymentStatus(PaymentStatus.FAILED);
        String reason = request.getErrorDescription();
        if (reason == null || reason.trim().isEmpty()) {
            reason = request.getErrorReason();
        }
        if (reason == null || reason.trim().isEmpty()) {
            reason = "Payment failed during checkout";
        }
        if (request.getErrorCode() != null && !request.getErrorCode().trim().isEmpty()) {
            reason = "[" + request.getErrorCode() + "] " + reason;
        }
        payment.setFailureReason(reason);

        Payment savedPayment = paymentRepository.save(payment);
        return PaymentResponse.fromEntity(savedPayment);
    }

    // ==========================================
    // 2C. SIMULATED TEST PAYMENTS (FOR DEVELOPMENT/PLACEHOLDER MODE)
    // ==========================================

    @Transactional
    public PaymentResponse simulatePaymentSuccess(String customerEmail, Long orderId) {
        User customer = getCustomer(customerEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        validateOrderOwnership(order, customer);
        validateOrderEligibilityForPayment(order);

        // Find existing pending payment or create one
        Payment payment = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PENDING)
                .findFirst()
                .orElse(null);

        if (payment == null) {
            RazorpayOrderResponse rzpOrder = createRazorpayOrder(customerEmail, orderId);
            payment = paymentRepository.findByRazorpayOrderId(rzpOrder.getRazorpayOrderId())
                    .orElseThrow(() -> new IllegalStateException("Failed to retrieve created payment attempt"));
        }

        String simulatedPaymentId = "pay_sim_" + System.currentTimeMillis();
        long amountInPaise = payment.getAmount().multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValue();

        // Register mock payment details for authoritative fetchPayment validation
        razorpayClientWrapper.registerMockPayment(
                simulatedPaymentId,
                "captured",
                "upi",
                amountInPaise,
                payment.getCurrency()
        );

        String signature = razorpayClientWrapper.computePaymentSignature(
                payment.getRazorpayOrderId(),
                simulatedPaymentId
        );

        return verifyPayment(customerEmail, new VerifyPaymentRequest(
                payment.getRazorpayOrderId(),
                simulatedPaymentId,
                signature
        ));
    }

    @Transactional
    public PaymentResponse simulatePaymentFailure(String customerEmail, Long orderId) {
        User customer = getCustomer(customerEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        validateOrderOwnership(order, customer);

        Payment payment = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PENDING)
                .findFirst()
                .orElse(null);

        if (payment == null) {
            RazorpayOrderResponse rzpOrder = createRazorpayOrder(customerEmail, orderId);
            payment = paymentRepository.findByRazorpayOrderId(rzpOrder.getRazorpayOrderId())
                    .orElseThrow(() -> new IllegalStateException("Failed to retrieve created payment attempt"));
        }

        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setFailureReason("Declined by customer in test payment simulation");
        Payment saved = paymentRepository.save(payment);
        return PaymentResponse.fromEntity(saved);
    }

    // ==========================================
    // 3. RETRY PAYMENT
    // ==========================================

    @Transactional
    public RazorpayOrderResponse retryPayment(String customerEmail, Long orderId) {
        User customer = getCustomer(customerEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        validateOrderOwnership(order, customer);
        validateOrderEligibilityForPayment(order);

        // Mark previous pending payment attempts as superseded so they do not linger
        List<Payment> previousPayments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        for (Payment p : previousPayments) {
            if (p.getPaymentStatus() == PaymentStatus.PENDING) {
                p.setPaymentStatus(PaymentStatus.FAILED);
                p.setFailureReason("Superseded by new payment retry");
                paymentRepository.save(p);
            }
        }

        // Re-initiating payment for the same order creates a new Razorpay attempt without creating a new Cravely order
        return createRazorpayOrder(customerEmail, orderId);
    }

    // ==========================================
    // 4. GET PAYMENT HISTORY FOR ORDER
    // ==========================================

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentHistoryByOrder(String customerEmail, Long orderId) {
        User customer = getCustomer(customerEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        validateOrderOwnership(order, customer);

        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 5. RAZORPAY WEBHOOK PROCESSING
    // ==========================================

    @Transactional
    public void processWebhook(String rawPayload, String signatureHeader) {
        if (!razorpayClientWrapper.verifyWebhookSignature(rawPayload, signatureHeader)) {
            throw new IllegalArgumentException("Invalid Razorpay webhook signature");
        }

        JSONObject json = new JSONObject(rawPayload);

        // Deduplication using event ID
        String eventId = json.optString("event_id", null);
        if (eventId == null) {
            eventId = json.optString("id", null);
        }

        if (eventId != null && webhookEventRepository.existsByEventId(eventId)) {
            // Already processed this webhook event
            return;
        }

        String eventType = json.optString("event", "");

        if ("payment.captured".equalsIgnoreCase(eventType)) {
            JSONObject paymentEntity = json.optJSONObject("payload") != null
                    ? json.optJSONObject("payload").optJSONObject("payment") != null
                    ? json.optJSONObject("payload").optJSONObject("payment").optJSONObject("entity") : null : null;

            if (paymentEntity != null) {
                String razorpayOrderId = paymentEntity.optString("order_id", null);
                String razorpayPaymentId = paymentEntity.optString("id", null);
                String method = paymentEntity.optString("method", null);
                long amountInPaise = paymentEntity.optLong("amount", -1);

                if (razorpayOrderId != null) {
                    paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(payment -> {
                        Order order = payment.getOrder();

                        // Idempotency: if already marked PAID, ignore duplicate processing
                        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
                            return;
                        }

                        // Amount check if present
                        if (amountInPaise > 0) {
                            long expectedPaise = payment.getAmount().multiply(new BigDecimal("100"))
                                    .setScale(0, RoundingMode.UNNECESSARY)
                                    .longValue();
                            if (amountInPaise != expectedPaise) {
                                payment.setPaymentStatus(PaymentStatus.FAILED);
                                payment.setFailureReason("Webhook payment amount mismatch: expected " + expectedPaise + " but received " + amountInPaise);
                                paymentRepository.save(payment);
                                return;
                            }
                        }

                        // Genuinely captured payment must be recorded as PAID with Razorpay payment ID
                        payment.setRazorpayPaymentId(razorpayPaymentId);
                        payment.setPaymentMethod(method);
                        payment.setPaymentStatus(PaymentStatus.PAID);
                        payment.setPaidAt(LocalDateTime.now());

                        // If the order has already been CANCELLED or REJECTED:
                        // 1. Record payment as PAID (funds are captured)
                        // 2. Preserve order's existing operational status (never reactivate)
                        // 3. Update order's paymentStatus to PAID
                        // 4. Flag the transaction for manual reconciliation/refund
                        if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.REJECTED) {
                            payment.setFailureReason("REQUIRES_REFUND: Payment captured after order was " + order.getOrderStatus() + ". Flagged for manual reconciliation/refund.");
                        }

                        paymentRepository.save(payment);

                        order.setPaymentStatus(PaymentStatus.PAID);
                        orderRepository.save(order);
                    });
                }
            }
        } else if ("payment.failed".equalsIgnoreCase(eventType)) {
            JSONObject paymentEntity = json.optJSONObject("payload") != null
                    ? json.optJSONObject("payload").optJSONObject("payment") != null
                    ? json.optJSONObject("payload").optJSONObject("payment").optJSONObject("entity") : null : null;

            if (paymentEntity != null) {
                String razorpayOrderId = paymentEntity.optString("order_id", null);
                String errorDescription = paymentEntity.optString("error_description", "Payment failed");

                if (razorpayOrderId != null) {
                    paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(payment -> {
                        if (payment.getPaymentStatus() != PaymentStatus.PAID) {
                            payment.setPaymentStatus(PaymentStatus.FAILED);
                            payment.setFailureReason(errorDescription);
                            paymentRepository.save(payment);
                        }
                    });
                }
            }
        } else if ("order.paid".equalsIgnoreCase(eventType)) {
            JSONObject orderEntity = json.optJSONObject("payload") != null
                    ? json.optJSONObject("payload").optJSONObject("order") != null
                    ? json.optJSONObject("payload").optJSONObject("order").optJSONObject("entity") : null : null;

            if (orderEntity != null) {
                String razorpayOrderId = orderEntity.optString("id", null);
                if (razorpayOrderId != null) {
                    paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(payment -> {
                        Order order = payment.getOrder();

                        if (payment.getPaymentStatus() != PaymentStatus.PAID) {
                            payment.setPaymentStatus(PaymentStatus.PAID);
                            payment.setPaidAt(LocalDateTime.now());

                            if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.REJECTED) {
                                payment.setFailureReason("REQUIRES_REFUND: Order paid after order was " + order.getOrderStatus() + ". Flagged for manual reconciliation/refund.");
                            }

                            paymentRepository.save(payment);

                            order.setPaymentStatus(PaymentStatus.PAID);
                            orderRepository.save(order);
                        }
                    });
                }
            }
        }

        // Record processed webhook event
        if (eventId != null) {
            webhookEventRepository.save(new WebhookEvent(eventId, eventType));
        }
    }

    // ==========================================
    // HELPER & VALIDATION METHODS
    // ==========================================

    private User getCustomer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer account not found"));

        if (user.getRole() != Role.CUSTOMER) {
            throw new AccessDeniedException("Only customers are authorized to perform payment operations");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("Customer account is deactivated");
        }

        return user;
    }

    private void validateOrderOwnership(Order order, User customer) {
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Order not found with id: " + order.getId());
        }
    }

    private void validateOrderEligibilityForPayment(Order order) {
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Order is already paid");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot initiate payment for a cancelled order");
        }

        if (order.getOrderStatus() == OrderStatus.REJECTED) {
            throw new IllegalStateException("Cannot initiate payment for a rejected order");
        }
    }
}
