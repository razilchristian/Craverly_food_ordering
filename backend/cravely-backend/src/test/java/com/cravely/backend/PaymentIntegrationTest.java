package com.cravely.backend;

import com.cravely.backend.dto.*;
import com.cravely.backend.entity.*;
import com.cravely.backend.exception.ResourceNotFoundException;
import com.cravely.backend.repository.PaymentRepository;
import com.cravely.backend.repository.UserRepository;
import com.cravely.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    // ==========================================
    // 1. PAYMENT INITIATION TESTS
    // ==========================================

    @Test
    void testCreateRazorpayOrder_Success() {
        RazorpayOrderResponse response = paymentService.createRazorpayOrder(customer1Email, order1Id);

        assertNotNull(response);
        assertNotNull(response.getRazorpayOrderId());
        assertNotNull(response.getRazorpayKeyId());
        assertEquals(90000L, response.getAmount()); // 900.00 converted to 90000 paise
        assertEquals(expectedOrderTotal, response.getAmountInRupees());
        assertEquals("INR", response.getCurrency());
        assertEquals(order1Id, response.getOrderId());

        // Verify Payment record in MySQL
        var payments = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order1Id);
        assertFalse(payments.isEmpty());
        Payment saved = payments.get(0);
        assertEquals(response.getRazorpayOrderId(), saved.getRazorpayOrderId());
        assertEquals(PaymentStatus.PENDING, saved.getPaymentStatus());
        assertEquals(expectedOrderTotal, saved.getAmount());
    }

    @Test
    void testAmountConvertsToPaiseCorrectly() {
        RazorpayOrderResponse response = paymentService.createRazorpayOrder(customer1Email, order1Id);
        // ₹900.00 * 100 = 90000 paise
        assertEquals(90000L, response.getAmount());
    }

    @Test
    void testCreateRazorpayOrder_CannotInitiateForAnotherCustomer() {
        // Customer 2 attempts to initiate payment for Customer 1's order
        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.createRazorpayOrder(customer2Email, order1Id)
        );
    }

    @Test
    void testCreateRazorpayOrder_AlreadyPaid_ThrowsException() {
        // Pay the order first
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String signature = razorpayClientWrapper.computePaymentSignature(rzpOrder.getRazorpayOrderId(), "pay_test123");
        paymentService.verifyPayment(customer1Email, new VerifyPaymentRequest(rzpOrder.getRazorpayOrderId(), "pay_test123", signature));

        // Attempting to initiate payment on already-paid order must fail
        assertThrows(IllegalStateException.class, () ->
                paymentService.createRazorpayOrder(customer1Email, order1Id)
        );
    }

    @Test
    void testCreateRazorpayOrder_CancelledOrder_ThrowsException() {
        orderService.cancelCustomerOrder(customer1Email, order1Id, new CancelOrderRequest("Mistake order"));

        assertThrows(IllegalStateException.class, () ->
                paymentService.createRazorpayOrder(customer1Email, order1Id)
        );
    }

    @Test
    void testCreateRazorpayOrder_InvalidOrderId_ThrowsException() {
        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.createRazorpayOrder(customer1Email, 999999L)
        );
    }

    @Test
    void testDuplicateInitiation_HandledSafely() {
        RazorpayOrderResponse r1 = paymentService.createRazorpayOrder(customer1Email, order1Id);
        RazorpayOrderResponse r2 = paymentService.createRazorpayOrder(customer1Email, order1Id);

        assertNotNull(r1.getRazorpayOrderId());
        assertNotNull(r2.getRazorpayOrderId());

        var list = paymentRepository.findByOrderIdOrderByCreatedAtDesc(order1Id);
        assertEquals(2, list.size());
    }

    // ==========================================
    // 2. SIGNATURE VERIFICATION TESTS
    // ==========================================

    @Test
    void testVerifyPayment_ValidSignature_MarksPaid() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String paymentId = "pay_valid_" + System.currentTimeMillis();
        String signature = razorpayClientWrapper.computePaymentSignature(rzpOrder.getRazorpayOrderId(), paymentId);

        PaymentResponse verified = paymentService.verifyPayment(customer1Email, new VerifyPaymentRequest(
                rzpOrder.getRazorpayOrderId(), paymentId, signature
        ));

        assertNotNull(verified);
        assertEquals(PaymentStatus.PAID, verified.getPaymentStatus());
        assertEquals(paymentId, verified.getRazorpayPaymentId());
        assertNotNull(verified.getPaidAt());

        // Check Cravely order status
        OrderResponse orderDetails = orderService.getCustomerOrderDetails(customer1Email, order1Id);
        assertEquals(PaymentStatus.PAID, orderDetails.getPaymentStatus());
    }

    @Test
    void testVerifyPayment_InvalidSignature_Fails() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);

        assertThrows(IllegalArgumentException.class, () ->
                paymentService.verifyPayment(customer1Email, new VerifyPaymentRequest(
                        rzpOrder.getRazorpayOrderId(), "pay_fake123", "fake_signature_abc123"
                ))
        );

        Payment payment = paymentRepository.findByRazorpayOrderId(rzpOrder.getRazorpayOrderId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());
        assertEquals("Invalid Razorpay payment signature", payment.getFailureReason());
    }

    @Test
    void testVerifyPayment_AlteredRazorpayOrderId_ThrowsNotFound() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String signature = razorpayClientWrapper.computePaymentSignature(rzpOrder.getRazorpayOrderId(), "pay_123");

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.verifyPayment(customer1Email, new VerifyPaymentRequest(
                        "order_altered_fake", "pay_123", signature
                ))
        );
    }

    @Test
    void testVerifyPayment_AnotherCustomerCannotVerify() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String paymentId = "pay_cross_" + System.currentTimeMillis();
        String signature = razorpayClientWrapper.computePaymentSignature(rzpOrder.getRazorpayOrderId(), paymentId);

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.verifyPayment(customer2Email, new VerifyPaymentRequest(
                        rzpOrder.getRazorpayOrderId(), paymentId, signature
                ))
        );
    }

    @Test
    void testVerifyPayment_DuplicateVerificationIsIdempotent() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String paymentId = "pay_idem_" + System.currentTimeMillis();
        String signature = razorpayClientWrapper.computePaymentSignature(rzpOrder.getRazorpayOrderId(), paymentId);

        VerifyPaymentRequest req = new VerifyPaymentRequest(rzpOrder.getRazorpayOrderId(), paymentId, signature);
        PaymentResponse v1 = paymentService.verifyPayment(customer1Email, req);
        PaymentResponse v2 = paymentService.verifyPayment(customer1Email, req);

        assertEquals(v1.getId(), v2.getId());
        assertEquals(PaymentStatus.PAID, v2.getPaymentStatus());
    }

    // ==========================================
    // 3. WEBHOOK PROCESSING TESTS
    // ==========================================

    @Test
    void testWebhook_PaymentCaptured_Success() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String paymentId = "pay_hook_" + System.currentTimeMillis();
        String eventId = "evt_" + System.currentTimeMillis();

        String payload = String.format(
                "{\"event_id\":\"%s\",\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"%s\",\"order_id\":\"%s\",\"method\":\"upi\",\"status\":\"captured\"}}}}",
                eventId, paymentId, rzpOrder.getRazorpayOrderId()
        );
        String signature = razorpayClientWrapper.computeWebhookSignature(payload);

        paymentService.processWebhook(payload, signature);

        Payment payment = paymentRepository.findByRazorpayOrderId(rzpOrder.getRazorpayOrderId()).orElseThrow();
        assertEquals(PaymentStatus.PAID, payment.getPaymentStatus());
        assertEquals(paymentId, payment.getRazorpayPaymentId());
        assertEquals("upi", payment.getPaymentMethod());

        OrderResponse order = orderService.getCustomerOrderDetails(customer1Email, order1Id);
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
    }

    @Test
    void testWebhook_InvalidSignature_ThrowsException() {
        String payload = "{\"event\":\"payment.captured\"}";
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.processWebhook(payload, "invalid_signature_hex")
        );
    }

    @Test
    void testWebhook_PaymentFailed_DoesNotMarkOrderPaid() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String eventId = "evt_fail_" + System.currentTimeMillis();

        String payload = String.format(
                "{\"event_id\":\"%s\",\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_fail_1\",\"order_id\":\"%s\",\"error_description\":\"Bank server down\"}}}}",
                eventId, rzpOrder.getRazorpayOrderId()
        );
        String signature = razorpayClientWrapper.computeWebhookSignature(payload);

        paymentService.processWebhook(payload, signature);

        Payment payment = paymentRepository.findByRazorpayOrderId(rzpOrder.getRazorpayOrderId()).orElseThrow();
        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());
        assertEquals("Bank server down", payment.getFailureReason());

        OrderResponse order = orderService.getCustomerOrderDetails(customer1Email, order1Id);
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
    }

    @Test
    void testWebhook_DuplicateEventId_HandledIdempotently() {
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String eventId = "evt_dup_" + System.currentTimeMillis();

        String payload = String.format(
                "{\"event_id\":\"%s\",\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_dup_1\",\"order_id\":\"%s\",\"method\":\"card\",\"status\":\"captured\"}}}}",
                eventId, rzpOrder.getRazorpayOrderId()
        );
        String signature = razorpayClientWrapper.computeWebhookSignature(payload);

        // First delivery
        paymentService.processWebhook(payload, signature);
        // Duplicate delivery should not throw or alter state
        assertDoesNotThrow(() -> paymentService.processWebhook(payload, signature));
    }

    // ==========================================
    // 4. RETRY & HISTORY TESTS
    // ==========================================

    @Test
    void testPaymentRetry_CreatesNewPaymentAttemptWithoutDuplicatingCravelyOrder() {
        RazorpayOrderResponse r1 = paymentService.createRazorpayOrder(customer1Email, order1Id);
        RazorpayOrderResponse retryRes = paymentService.retryPayment(customer1Email, order1Id);

        assertNotNull(retryRes);
        assertNotEquals(r1.getRazorpayOrderId(), retryRes.getRazorpayOrderId());
        assertEquals(order1Id, retryRes.getOrderId());

        var history = paymentService.getPaymentHistoryByOrder(customer1Email, order1Id);
        assertEquals(2, history.size());
    }

    @Test
    void testPaymentRetry_PaidOrder_ThrowsException() {
        RazorpayOrderResponse r1 = paymentService.createRazorpayOrder(customer1Email, order1Id);
        String sig = razorpayClientWrapper.computePaymentSignature(r1.getRazorpayOrderId(), "pay_paid");
        paymentService.verifyPayment(customer1Email, new VerifyPaymentRequest(r1.getRazorpayOrderId(), "pay_paid", sig));

        assertThrows(IllegalStateException.class, () ->
                paymentService.retryPayment(customer1Email, order1Id)
        );
    }
}
