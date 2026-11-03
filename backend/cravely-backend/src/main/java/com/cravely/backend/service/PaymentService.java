package com.cravely.backend.service;

import com.cravely.backend.config.RazorpayProperties;
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

        // Save Payment attempt in MySQL
        Payment payment = new Payment(order, rzpOrder.id(), totalAmount, currency);
        paymentRepository.save(payment);

        return new RazorpayOrderResponse(
                rzpOrder.id(),
                razorpayProperties.getKeyId(),
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

        validateOrderOwnership(payment.getOrder(), customer);

        // Idempotency: if already marked PAID, return existing verified response
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            return PaymentResponse.fromEntity(payment);
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

        if ("captured".equalsIgnoreCase(status) || "authorized".equalsIgnoreCase(status)) {
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setRazorpaySignature(request.getRazorpaySignature());
            payment.setPaymentStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setPaymentMethod(paymentDetails.method());
            Payment savedPayment = paymentRepository.save(payment);

            Order order = payment.getOrder();
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);

            return PaymentResponse.fromEntity(savedPayment);
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Razorpay payment status is not captured: " + status);
            paymentRepository.save(payment);
            throw new IllegalStateException("Payment status is not captured: " + status);
        }
    }

    // ==========================================
    // 3. RETRY PAYMENT
    // ==========================================

    @Transactional
    public RazorpayOrderResponse retryPayment(String customerEmail, Long orderId) {
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
            JSONObject paymentEntity = json.optJSONObject("payload")
                    .optJSONObject("payment")
                    .optJSONObject("entity");

            if (paymentEntity != null) {
                String razorpayOrderId = paymentEntity.optString("order_id", null);
                String razorpayPaymentId = paymentEntity.optString("id", null);
                String method = paymentEntity.optString("method", null);

                if (razorpayOrderId != null) {
                    paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(payment -> {
                        if (payment.getPaymentStatus() != PaymentStatus.PAID) {
                            payment.setRazorpayPaymentId(razorpayPaymentId);
                            payment.setPaymentMethod(method);
                            payment.setPaymentStatus(PaymentStatus.PAID);
                            payment.setPaidAt(LocalDateTime.now());
                            paymentRepository.save(payment);

                            Order order = payment.getOrder();
                            order.setPaymentStatus(PaymentStatus.PAID);
                            orderRepository.save(order);
                        }
                    });
                }
            }
        } else if ("payment.failed".equalsIgnoreCase(eventType)) {
            JSONObject paymentEntity = json.optJSONObject("payload")
                    .optJSONObject("payment")
                    .optJSONObject("entity");

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
            JSONObject orderEntity = json.optJSONObject("payload")
                    .optJSONObject("order")
                    .optJSONObject("entity");

            if (orderEntity != null) {
                String razorpayOrderId = orderEntity.optString("id", null);
                if (razorpayOrderId != null) {
                    paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(payment -> {
                        if (payment.getPaymentStatus() != PaymentStatus.PAID) {
                            payment.setPaymentStatus(PaymentStatus.PAID);
                            payment.setPaidAt(LocalDateTime.now());
                            paymentRepository.save(payment);

                            Order order = payment.getOrder();
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
