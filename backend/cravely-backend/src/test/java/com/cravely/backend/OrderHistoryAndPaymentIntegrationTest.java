package com.cravely.backend;

import com.cravely.backend.dto.*;
import com.cravely.backend.entity.OrderStatus;
import com.cravely.backend.entity.Payment;
import com.cravely.backend.entity.PaymentStatus;
import com.cravely.backend.entity.Role;
import com.cravely.backend.repository.PaymentRepository;
import com.cravely.backend.service.AuthService;
import com.cravely.backend.service.CartService;
import com.cravely.backend.service.OrderService;
import com.cravely.backend.service.PaymentService;
import com.cravely.backend.service.RazorpayClientWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class OrderHistoryAndPaymentIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RazorpayClientWrapper razorpayClientWrapper;

    private String customer1Email;
    private String customer2Email;
    private Long order1Id;

    @BeforeEach
    void setup() {
        customer1Email = "oh_cust1_" + System.currentTimeMillis() + "@example.com";
        customer2Email = "oh_cust2_" + System.currentTimeMillis() + "@example.com";

        authService.register(new RegisterRequest("Order Customer 1", customer1Email, "9988776651", "Password123!", Role.CUSTOMER));
        authService.register(new RegisterRequest("Order Customer 2", customer2Email, "9988776652", "Password123!", Role.CUSTOMER));

        // Place an order for customer 1
        cartService.syncCart(customer1Email, new SyncCartRequest(List.of(
                new SyncCartItemRequest(null, "f4", "Paneer Tikka Pizza", "pizza-theory", 1)
        )));

        OrderResponse placed = orderService.placeOrder(customer1Email, new PlaceOrderRequest(
                "101 Green Park", "Ahmedabad", "Gujarat", "380015", "9988776651", "Ring the bell", "WELCOME10"
        ));
        order1Id = placed.getId();
    }

    @Test
    @DisplayName("1. Authenticated customer can retrieve order history with pagination")
    void testCustomerOrderHistoryRetrieval() {
        Page<OrderSummaryResponse> orders = orderService.getMyOrders(
                customer1Email, null, null, null, PageRequest.of(0, 10)
        );

        assertNotNull(orders);
        assertEquals(1, orders.getTotalElements());
        OrderSummaryResponse summary = orders.getContent().get(0);
        assertEquals(order1Id, summary.getId());
        assertEquals("Pizza Theory", summary.getRestaurantName());
        assertEquals(OrderStatus.PLACED, summary.getOrderStatus());
        assertEquals(PaymentStatus.PENDING, summary.getPaymentStatus());
    }

    @Test
    @DisplayName("2. Order history enforces tenant isolation between customers")
    void testOrderHistoryTenantIsolation() {
        Page<OrderSummaryResponse> cust2Orders = orderService.getMyOrders(
                customer2Email, null, null, null, PageRequest.of(0, 10)
        );

        assertNotNull(cust2Orders);
        assertEquals(0, cust2Orders.getTotalElements());
    }

    @Test
    @DisplayName("3. Create payment order initiates Razorpay order attempt in MySQL")
    void testCreatePaymentOrder() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);

        assertNotNull(rzpOrder);
        assertNotNull(rzpOrder.getRazorpayOrderId());
        assertEquals(order1Id, rzpOrder.getOrderId());
        assertEquals("INR", rzpOrder.getCurrency());
        assertTrue(rzpOrder.getAmount() > 0);

        Payment payment = paymentRepository.findByRazorpayOrderId(rzpOrder.getRazorpayOrderId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus());
        assertEquals(order1Id, payment.getOrder().getId());
    }

    @Test
    @DisplayName("4. Payment signature validation accepts genuine HMAC-SHA256 signature")
    void testPaymentSignatureValidationAcceptsValid() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String paymentId = "pay_test_" + System.currentTimeMillis();
        String validSignature = razorpayClientWrapper.computePaymentSignature(rzpOrder.getRazorpayOrderId(), paymentId);

        assertTrue(razorpayClientWrapper.verifyPaymentSignature(rzpOrder.getRazorpayOrderId(), paymentId, validSignature));

        PaymentResponse verified = paymentService.verifyPayment(customer1Email, new VerifyPaymentRequest(
                rzpOrder.getRazorpayOrderId(), paymentId, validSignature
        ));

        assertEquals(PaymentStatus.PAID, verified.getPaymentStatus());
        assertEquals(paymentId, verified.getRazorpayPaymentId());

        // Order is also marked PAID
        OrderResponse orderDetails = orderService.getCustomerOrderDetails(customer1Email, order1Id);
        assertEquals(PaymentStatus.PAID, orderDetails.getPaymentStatus());
        assertEquals(OrderStatus.PLACED, orderDetails.getOrderStatus()); // Operational status preserved
    }

    @Test
    @DisplayName("5. Payment signature validation rejects tampered signature and records FAILED")
    void testPaymentSignatureValidationRejectsTampered() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);

        assertThrows(IllegalArgumentException.class, () ->
                paymentService.verifyPayment(customer1Email, new VerifyPaymentRequest(
                        rzpOrder.getRazorpayOrderId(), "pay_tampered", "invalid_signature_hex"
                ))
        );

        Payment payment = paymentRepository.findByRazorpayOrderId(rzpOrder.getRazorpayOrderId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());
        assertEquals("Invalid Razorpay payment signature", payment.getFailureReason());

        // Order remains in PENDING for retry
        OrderResponse orderDetails = orderService.getCustomerOrderDetails(customer1Email, order1Id);
        assertEquals(PaymentStatus.PENDING, orderDetails.getPaymentStatus());
    }

    @Test
    @DisplayName("6. Client failure callback records payment failure with description")
    void testRecordPaymentFailureCallback() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);

        PaymentResponse failureResponse = paymentService.recordPaymentFailure(customer1Email, new PaymentFailureRequest(
                rzpOrder.getRazorpayOrderId(),
                "pay_failed_123",
                "BAD_REQUEST_ERROR",
                "Payment was declined by issuing bank",
                "card_declined"
        ));

        assertEquals(PaymentStatus.FAILED, failureResponse.getPaymentStatus());
        assertTrue(failureResponse.getFailureReason().contains("Payment was declined by issuing bank"));

        Payment payment = paymentRepository.findByRazorpayOrderId(rzpOrder.getRazorpayOrderId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());
    }

    @Test
    @DisplayName("7. Simulated test payment success transitions payment and order to PAID")
    void testSimulatedTestPaymentSuccess() {
        PaymentResponse response = paymentService.simulatePaymentSuccess(customer1Email, order1Id);

        assertNotNull(response);
        assertEquals(PaymentStatus.PAID, response.getPaymentStatus());
        assertNotNull(response.getRazorpayPaymentId());

        OrderResponse orderDetails = orderService.getCustomerOrderDetails(customer1Email, order1Id);
        assertEquals(PaymentStatus.PAID, orderDetails.getPaymentStatus());
    }

    @Test
    @DisplayName("8. Simulated test payment failure marks payment FAILED without affecting order PLACED status")
    void testSimulatedTestPaymentFailure() {
        PaymentResponse response = paymentService.simulatePaymentFailure(customer1Email, order1Id);

        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());

        OrderResponse orderDetails = orderService.getCustomerOrderDetails(customer1Email, order1Id);
        assertEquals(PaymentStatus.PENDING, orderDetails.getPaymentStatus());
        assertEquals(OrderStatus.PLACED, orderDetails.getOrderStatus());
    }

    @Test
    @DisplayName("9. Payment retry creates a new attempt for the same order without creating duplicate orders")
    void testPaymentRetryPreservesOrder() {
        RazorpayOrderResponse firstAttempt = paymentService.createRazorpayOrder(customer1Email, order1Id);
        paymentService.simulatePaymentFailure(customer1Email, order1Id);

        // Retry payment
        RazorpayOrderResponse secondAttempt = paymentService.retryPayment(customer1Email, order1Id);

        assertNotNull(secondAttempt);
        assertEquals(order1Id, secondAttempt.getOrderId());
        assertNotEquals(firstAttempt.getRazorpayOrderId(), secondAttempt.getRazorpayOrderId());

        // Previous attempt marked FAILED with reason
        Payment prev = paymentRepository.findByRazorpayOrderId(firstAttempt.getRazorpayOrderId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, prev.getPaymentStatus());

        // New attempt is PENDING
        Payment current = paymentRepository.findByRazorpayOrderId(secondAttempt.getRazorpayOrderId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, current.getPaymentStatus());

        // Total orders for customer remains exactly 1
        Page<OrderSummaryResponse> orders = orderService.getMyOrders(
                customer1Email, null, null, null, PageRequest.of(0, 10)
        );
        assertEquals(1, orders.getTotalElements());
    }
}

