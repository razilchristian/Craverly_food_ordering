package com.cravely.backend;

import com.cravely.backend.dto.*;
import com.cravely.backend.entity.*;
import com.cravely.backend.exception.ResourceNotFoundException;
import com.cravely.backend.repository.PaymentRepository;
import com.cravely.backend.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PaymentIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private FoodCategoryService categoryService;

    @Autowired
    private FoodItemService foodItemService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RazorpayClientWrapper razorpayClientWrapper;

    private String customer1Email;
    private String customer2Email;
    private String ownerEmail;

    private Long order1Id;
    private BigDecimal expectedOrderTotal;

    @BeforeEach
    void setup() {
        long ts = System.currentTimeMillis();
        customer1Email = "pay_cust1_" + ts + "@example.com";
        customer2Email = "pay_cust2_" + ts + "@example.com";
        ownerEmail = "pay_own_" + ts + "@example.com";

        authService.register(new RegisterRequest("Pay Cust 1", customer1Email, "2223334441", "Password123!", Role.CUSTOMER));
        authService.register(new RegisterRequest("Pay Cust 2", customer2Email, "2223334442", "Password123!", Role.CUSTOMER));
        authService.register(new RegisterRequest("Pay Owner", ownerEmail, "2223334443", "Password123!", Role.RESTAURANT_OWNER));

        // Create & approve Restaurant
        var r = restaurantService.createRestaurant(ownerEmail, new RestaurantCreateRequest(
                "Royal Biryani", "Mughlai & Biryani", "88 Palace Rd", "Ahmedabad", "Gujarat", "380001",
                "9876543299", "royal@biryani.com", "Mughlai", null, LocalTime.of(10, 0), LocalTime.of(23, 0)
        ));
        restaurantService.approveRestaurant(r.getId());

        // Create category and food item
        var cat = categoryService.createCategory(ownerEmail, r.getId(), new FoodCategoryCreateRequest("Biryani", "Dum biryani"));
        var item = foodItemService.createFoodItem(ownerEmail, r.getId(), new FoodItemCreateRequest(
                "Hyderabadi Dum Biryani", "Aromatic basmati rice", cat.getId(), new BigDecimal("450.00"), null, FoodType.NON_VEG, true, 30
        ));

        // Customer 1 adds item to cart & places order
        cartService.addToCart(customer1Email, new AddToCartRequest(item.getId(), 2));
        expectedOrderTotal = new BigDecimal("900.00"); // 2 * 450.00

        OrderResponse placedOrder = orderService.placeOrder(customer1Email, new PlaceOrderRequest(
                "404 Sapphire Heights", "Ahmedabad", "Gujarat", "380015", "9876543299", "Fragile handle with care"
        ));
        order1Id = placedOrder.getId();
    }

    @AfterEach
    void tearDown() {
        razorpayClientWrapper.clearMockPayments();
    }

    // ========================================================
    // 15 REQUIRED AUDIT & STABILIZATION PAYMENT TESTS
    // ========================================================

    // 1. Payment order creation for a valid order
    @Test
    void test1_PaymentOrderCreation_ForValidOrder_Success() {
        RazorpayOrderResponse response = paymentService.createRazorpayOrder(customer1Email, order1Id);

        assertNotNull(response);
        assertNotNull(response.getRazorpayOrderId());
        assertNotNull(response.getRazorpayKeyId());
        assertEquals(90000L, response.getAmount()); // 900.00 converted to 90000 paise
        assertEquals(expectedOrderTotal, response.getAmountInRupees());
        assertEquals("INR", response.getCurrency());
        assertEquals(order1Id, response.getOrderId());
    }

    // 2. Payment creation for another customer's order is rejected
    @Test
    void test2_PaymentCreation_ForAnotherCustomerOrder_Rejected() {
        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.createRazorpayOrder(customer2Email, order1Id)
        );
    }

    // 3. Payment amount is derived from the database
    @Test
    void test3_PaymentAmount_DerivedFromDatabase() {
        RazorpayOrderResponse response = paymentService.createRazorpayOrder(customer1Email, order1Id);
        // DB total is 900.00 -> paise must be 90000
        assertEquals(90000L, response.getAmount());
        assertEquals(expectedOrderTotal, response.getAmountInRupees());
    }

    // 4. Invalid payment signature is rejected
    @Test
    void test4_InvalidPaymentSignature_Rejected() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);

        assertThrows(IllegalArgumentException.class, () ->
                paymentService.verifyPayment(customer1Email, new VerifyPaymentRequest(
                        rzpOrder.getRazorpayOrderId(), "pay_invalid_sig", "tampered_signature_hex"
                ))
        );

        Payment payment = paymentRepository.findByRazorpayOrderId(rzpOrder.getRazorpayOrderId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());
        assertEquals("Invalid Razorpay payment signature", payment.getFailureReason());
    }

    // 5. Valid payment signature is accepted
    @Test
    void test5_ValidPaymentSignature_Accepted() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String paymentId = "pay_valid_" + System.currentTimeMillis();
        String signature = razorpayClientWrapper.computePaymentSignature(rzpOrder.getRazorpayOrderId(), paymentId);

        PaymentResponse verified = paymentService.verifyPayment(customer1Email, new VerifyPaymentRequest(
                rzpOrder.getRazorpayOrderId(), paymentId, signature
        ));

        assertNotNull(verified);
        assertEquals(PaymentStatus.PAID, verified.getPaymentStatus());
        assertEquals(paymentId, verified.getRazorpayPaymentId());
    }

    // 6. Captured payment becomes PAID
    @Test
    void test6_CapturedPayment_BecomesPaid() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String paymentId = "pay_cap_" + System.currentTimeMillis();
        String signature = razorpayClientWrapper.computePaymentSignature(rzpOrder.getRazorpayOrderId(), paymentId);

        // Register captured payment mock
        razorpayClientWrapper.registerMockPayment(paymentId, "captured", "upi", 90000L, "INR");

        PaymentResponse response = paymentService.verifyPayment(customer1Email, new VerifyPaymentRequest(
                rzpOrder.getRazorpayOrderId(), paymentId, signature
        ));

        assertEquals(PaymentStatus.PAID, response.getPaymentStatus());
        OrderResponse orderDetails = orderService.getCustomerOrderDetails(customer1Email, order1Id);
        assertEquals(PaymentStatus.PAID, orderDetails.getPaymentStatus());
        // Verify OrderStatus is preserved as PLACED (not overwritten)
        assertEquals(OrderStatus.PLACED, orderDetails.getOrderStatus());
    }

    // 7. Authorized payment does not become PAID prematurely
    @Test
    void test7_AuthorizedPayment_DoesNotBecomePaidPrematurely() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String paymentId = "pay_auth_" + System.currentTimeMillis();
        String signature = razorpayClientWrapper.computePaymentSignature(rzpOrder.getRazorpayOrderId(), paymentId);

        // Register authorized payment (not captured!)
        razorpayClientWrapper.registerMockPayment(paymentId, "authorized", "card", 90000L, "INR");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                paymentService.verifyPayment(customer1Email, new VerifyPaymentRequest(
                        rzpOrder.getRazorpayOrderId(), paymentId, signature
                ))
        );

        assertTrue(ex.getMessage().contains("authorized but not yet captured"));

        // Verify payment is NOT PAID
        Payment payment = paymentRepository.findByRazorpayOrderId(rzpOrder.getRazorpayOrderId()).orElseThrow();
        assertNotEquals(PaymentStatus.PAID, payment.getPaymentStatus());

        OrderResponse orderDetails = orderService.getCustomerOrderDetails(customer1Email, order1Id);
        assertNotEquals(PaymentStatus.PAID, orderDetails.getPaymentStatus());
    }

    // 8. Failed payment is handled correctly
    @Test
    void test8_FailedPayment_HandledCorrectly() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String eventId = "evt_fail_" + System.currentTimeMillis();

        String payload = String.format(
                "{\"event_id\":\"%s\",\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_fail_1\",\"order_id\":\"%s\",\"error_description\":\"Insufficient funds\"}}}}",
                eventId, rzpOrder.getRazorpayOrderId()
        );
        String signature = razorpayClientWrapper.computeWebhookSignature(payload);

        paymentService.processWebhook(payload, signature);

        Payment payment = paymentRepository.findByRazorpayOrderId(rzpOrder.getRazorpayOrderId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());
        assertEquals("Insufficient funds", payment.getFailureReason());

        // Order remains preserved in PENDING payment status, ready for retry
        OrderResponse order = orderService.getCustomerOrderDetails(customer1Email, order1Id);
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(OrderStatus.PLACED, order.getOrderStatus());
    }

    // 9. Pending payment remains pending
    @Test
    void test9_PendingPayment_RemainsPending() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);

        Payment payment = paymentRepository.findByRazorpayOrderId(rzpOrder.getRazorpayOrderId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus());
        assertNull(payment.getPaidAt());

        OrderResponse order = orderService.getCustomerOrderDetails(customer1Email, order1Id);
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
    }

    // 10. Duplicate webhook event is ignored safely
    @Test
    void test10_DuplicateWebhookEvent_IgnoredSafely() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String eventId = "evt_dup_" + System.currentTimeMillis();

        String payload = String.format(
                "{\"event_id\":\"%s\",\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_dup_1\",\"order_id\":\"%s\",\"method\":\"card\",\"status\":\"captured\",\"amount\":90000}}}}",
                eventId, rzpOrder.getRazorpayOrderId()
        );
        String signature = razorpayClientWrapper.computeWebhookSignature(payload);

        // First delivery
        paymentService.processWebhook(payload, signature);
        // Duplicate delivery should not throw or alter state
        assertDoesNotThrow(() -> paymentService.processWebhook(payload, signature));
    }

    // 11. Invalid webhook signature is rejected
    @Test
    void test11_InvalidWebhookSignature_Rejected() {
        String payload = "{\"event\":\"payment.captured\"}";
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.processWebhook(payload, "bad_signature_hex")
        );
    }

    // 12. Payment retry does not create another order
    @Test
    void test12_PaymentRetry_DoesNotCreateAnotherOrder() {
        RazorpayOrderResponse r1 = paymentService.createRazorpayOrder(customer1Email, order1Id);
        RazorpayOrderResponse retryRes = paymentService.retryPayment(customer1Email, order1Id);

        assertNotNull(retryRes);
        assertNotEquals(r1.getRazorpayOrderId(), retryRes.getRazorpayOrderId());
        assertEquals(order1Id, retryRes.getOrderId());

        // Previous attempt marked FAILED, new attempt is PENDING
        var history = paymentService.getPaymentHistoryByOrder(customer1Email, order1Id);
        assertEquals(2, history.size());
        assertEquals(PaymentStatus.PENDING, history.get(0).getPaymentStatus()); // Most recent
        assertEquals(PaymentStatus.FAILED, history.get(1).getPaymentStatus());  // Superseded
    }

    // 13. Cancelled order is not reactivated by a delayed webhook
    @Test
    void test13_CancelledOrder_NotReactivatedByDelayedWebhook() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);

        // Customer cancels order before payment is processed
        orderService.cancelCustomerOrder(customer1Email, order1Id, new CancelOrderRequest("Changed mind"));

        OrderResponse cancelledOrder = orderService.getCustomerOrderDetails(customer1Email, order1Id);
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getOrderStatus());

        // Delayed webhook arrives from Razorpay
        String eventId = "evt_delayed_" + System.currentTimeMillis();
        String payload = String.format(
                "{\"event_id\":\"%s\",\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_delayed_1\",\"order_id\":\"%s\",\"method\":\"upi\",\"status\":\"captured\",\"amount\":90000}}}}",
                eventId, rzpOrder.getRazorpayOrderId()
        );
        String signature = razorpayClientWrapper.computeWebhookSignature(payload);

        paymentService.processWebhook(payload, signature);

        // Verify order remains strictly CANCELLED and was NOT reactivated
        OrderResponse orderAfterWebhook = orderService.getCustomerOrderDetails(customer1Email, order1Id);
        assertEquals(OrderStatus.CANCELLED, orderAfterWebhook.getOrderStatus());
        assertNotEquals(PaymentStatus.PAID, orderAfterWebhook.getPaymentStatus());
    }

    // 14. Payment amount mismatch is rejected
    @Test
    void test14_PaymentAmountMismatch_Rejected() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String paymentId = "pay_mismatch_" + System.currentTimeMillis();
        String signature = razorpayClientWrapper.computePaymentSignature(rzpOrder.getRazorpayOrderId(), paymentId);

        // Register mock payment with tampered amount (50000 paise instead of expected 90000 paise)
        razorpayClientWrapper.registerMockPayment(paymentId, "captured", "upi", 50000L, "INR");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                paymentService.verifyPayment(customer1Email, new VerifyPaymentRequest(
                        rzpOrder.getRazorpayOrderId(), paymentId, signature
                ))
        );

        assertTrue(ex.getMessage().contains("Payment amount mismatch"));

        Payment payment = paymentRepository.findByRazorpayOrderId(rzpOrder.getRazorpayOrderId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());
        assertTrue(payment.getFailureReason().contains("Payment amount mismatch"));
    }

    // 15. Duplicate verification requests are handled safely (idempotent)
    @Test
    void test15_DuplicateVerificationRequests_HandledSafely() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String paymentId = "pay_idem_" + System.currentTimeMillis();
        String signature = razorpayClientWrapper.computePaymentSignature(rzpOrder.getRazorpayOrderId(), paymentId);

        VerifyPaymentRequest req = new VerifyPaymentRequest(rzpOrder.getRazorpayOrderId(), paymentId, signature);
        PaymentResponse v1 = paymentService.verifyPayment(customer1Email, req);
        PaymentResponse v2 = paymentService.verifyPayment(customer1Email, req);

        assertEquals(v1.getId(), v2.getId());
        assertEquals(PaymentStatus.PAID, v2.getPaymentStatus());
    }

    // ========================================================
    // ADDITIONAL SECURITY & REGRESSION CHECKS
    // ========================================================

    @Test
    void testCannotVerifyPaymentForCancelledOrder() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        orderService.cancelCustomerOrder(customer1Email, order1Id, new CancelOrderRequest("Cancelled"));

        String paymentId = "pay_test_" + System.currentTimeMillis();
        String signature = razorpayClientWrapper.computePaymentSignature(rzpOrder.getRazorpayOrderId(), paymentId);

        assertThrows(IllegalStateException.class, () ->
                paymentService.verifyPayment(customer1Email, new VerifyPaymentRequest(
                        rzpOrder.getRazorpayOrderId(), paymentId, signature
                ))
        );
    }

    @Test
    void testCrossCustomerVerification_Forbidden() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String paymentId = "pay_cross_" + System.currentTimeMillis();
        String signature = razorpayClientWrapper.computePaymentSignature(rzpOrder.getRazorpayOrderId(), paymentId);

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.verifyPayment(customer2Email, new VerifyPaymentRequest(
                        rzpOrder.getRazorpayOrderId(), paymentId, signature
                ))
        );
    }
}
