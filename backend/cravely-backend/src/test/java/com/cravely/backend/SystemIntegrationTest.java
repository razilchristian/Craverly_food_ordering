package com.cravely.backend;

import com.cravely.backend.dto.*;
import com.cravely.backend.entity.*;
import com.cravely.backend.exception.ResourceNotFoundException;
import com.cravely.backend.repository.*;
import com.cravely.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SystemIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private FoodCategoryService categoryService;

    @Autowired
    private FoodItemService foodItemService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AdminDashboardService adminDashboardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderStatusHistoryRepository historyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User customer;
    private User customer2;
    private User owner;
    private User owner2;
    private User admin;

    private Long restaurantId;
    private Long foodItemId;

    @BeforeEach
    void setup() {
        long ts = System.currentTimeMillis();
        String custEmail = "sys_cust_" + ts + "@example.com";
        String cust2Email = "sys_cust2_" + ts + "@example.com";
        String ownEmail = "sys_own_" + ts + "@example.com";
        String own2Email = "sys_own2_" + ts + "@example.com";
        String admEmail = "sys_adm_" + ts + "@example.com";

        authService.register(new RegisterRequest("System Customer", custEmail, "9998881111", "Password123!", Role.CUSTOMER));
        authService.register(new RegisterRequest("Other Customer", cust2Email, "9998882222", "Password123!", Role.CUSTOMER));
        authService.register(new RegisterRequest("System Owner", ownEmail, "9998883333", "Password123!", Role.RESTAURANT_OWNER));
        authService.register(new RegisterRequest("Other Owner", own2Email, "9998884444", "Password123!", Role.RESTAURANT_OWNER));

        userRepository.save(new User("System Admin", admEmail, "9998885555", passwordEncoder.encode("Password123!"), Role.ADMIN, true));

        customer = userRepository.findByEmail(custEmail).orElseThrow();
        customer2 = userRepository.findByEmail(cust2Email).orElseThrow();
        owner = userRepository.findByEmail(ownEmail).orElseThrow();
        owner2 = userRepository.findByEmail(own2Email).orElseThrow();
        admin = userRepository.findByEmail(admEmail).orElseThrow();

        // Register and approve restaurant
        var r = restaurantService.createRestaurant(owner.getEmail(), new RestaurantCreateRequest(
                "System Spice", "Indian Mughlai", "101 Grand Trunk Rd", "Ahmedabad", "Gujarat", "380001",
                "9876543100", "spice@sys.com", "North Indian", null, LocalTime.of(9, 0), LocalTime.of(23, 0)
        ));
        restaurantService.approveRestaurant(r.getId());
        restaurantId = r.getId();

        // Create category and food item
        var cat = categoryService.createCategory(owner.getEmail(), restaurantId, new FoodCategoryCreateRequest("Biryani Special", "Fragrant rice delicacies"));
        var item = foodItemService.createFoodItem(owner.getEmail(), restaurantId, new FoodItemCreateRequest(
                "Royal Hyderabadi Dum Biryani", "Fragrant basmati rice cooked with rich spices",
                cat.getId(), new BigDecimal("350.00"), null, FoodType.NON_VEG, true, 25
        ));
        foodItemId = item.getId();
    }

    @Test
    @DisplayName("Module 9: Full End-to-End Customer Lifecycle (Cart, Order, Payment, Tracking)")
    void testFullCustomerOrderLifecycle() {
        // 1. Add item to cart
        CartResponse cart = cartService.addToCart(customer.getEmail(), new AddToCartRequest(foodItemId, 2));
        assertNotNull(cart);
        assertEquals(1, cart.getItems().size());
        assertEquals(new BigDecimal("700.00"), cart.getSubtotal());

        // 2. Place Order
        PlaceOrderRequest orderReq = new PlaceOrderRequest(
                "Flat 402, Royal Residency", "Ahmedabad", "Gujarat", "380015",
                "9998881111", "Less spicy please"
        );
        OrderResponse placedOrder = orderService.placeOrder(customer.getEmail(), orderReq);
        assertNotNull(placedOrder);
        assertEquals(OrderStatus.PLACED, placedOrder.getOrderStatus());
        assertEquals(PaymentStatus.PENDING, placedOrder.getPaymentStatus());
        assertEquals(new BigDecimal("700.00"), placedOrder.getTotalAmount());

        // Verify cart is cleared after order placement
        CartResponse emptyCart = cartService.getCart(customer.getEmail());
        assertTrue(emptyCart.getItems().isEmpty());

        // 3. Initiate and capture Payment
        RazorpayOrderResponse rzpOrder = paymentService.createRazorpayOrder(customer.getEmail(), placedOrder.getId());
        assertNotNull(rzpOrder.getRazorpayOrderId());

        Payment payment = paymentRepository.findByRazorpayOrderId(rzpOrder.getRazorpayOrderId()).orElseThrow();
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setRazorpayPaymentId("pay_sys_" + System.currentTimeMillis());
        paymentRepository.save(payment);

        Order order = orderRepository.findById(placedOrder.getId()).orElseThrow();
        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);

        // Verify Order status is now updated to PAID
        OrderResponse refreshedOrder = orderService.getCustomerOrderDetails(customer.getEmail(), placedOrder.getId());
        assertEquals(PaymentStatus.PAID, refreshedOrder.getPaymentStatus());

        // 4. Owner transitions order: ACCEPTED -> PREPARING -> READY_FOR_PICKUP
        orderService.acceptOrder(owner.getEmail(), placedOrder.getId());
        orderService.markOrderPreparing(owner.getEmail(), placedOrder.getId());
        orderService.markOrderReady(owner.getEmail(), placedOrder.getId());

        // 5. Verify Owner CANNOT perform delivery transitions (enforces Module 9 delivery authorization correction)
        assertThrows(AccessDeniedException.class, () ->
                orderService.markOrderOutForDeliveryByOwner(owner.getEmail(), placedOrder.getId())
        );
        assertThrows(AccessDeniedException.class, () ->
                orderService.markOrderDeliveredByOwner(owner.getEmail(), placedOrder.getId())
        );

        // 6. Admin performs delivery transitions: OUT_FOR_DELIVERY -> DELIVERED
        orderService.markOrderOutForDeliveryByAdmin(admin.getEmail(), placedOrder.getId());
        OrderResponse deliveredOrder = orderService.markOrderDeliveredByAdmin(admin.getEmail(), placedOrder.getId());
        assertEquals(OrderStatus.DELIVERED, deliveredOrder.getOrderStatus());

        // 7. Verify Order Tracking History timeline integrity
        List<OrderStatusHistoryResponse> tracking = orderService.getOrderTrackingHistory(customer.getEmail(), placedOrder.getId());
        assertEquals(6, tracking.size());

        assertEquals(OrderStatus.PLACED, tracking.get(0).getNewStatus());
        assertEquals("CUSTOMER", tracking.get(0).getChangedByRole());

        assertEquals(OrderStatus.ACCEPTED, tracking.get(1).getNewStatus());
        assertEquals("RESTAURANT_OWNER", tracking.get(1).getChangedByRole());

        assertEquals(OrderStatus.PREPARING, tracking.get(2).getNewStatus());
        assertEquals("RESTAURANT_OWNER", tracking.get(2).getChangedByRole());

        assertEquals(OrderStatus.READY_FOR_PICKUP, tracking.get(3).getNewStatus());
        assertEquals("RESTAURANT_OWNER", tracking.get(3).getChangedByRole());

        assertEquals(OrderStatus.OUT_FOR_DELIVERY, tracking.get(4).getNewStatus());
        assertEquals("ADMIN", tracking.get(4).getChangedByRole());

        assertEquals(OrderStatus.DELIVERED, tracking.get(5).getNewStatus());
        assertEquals("ADMIN", tracking.get(5).getChangedByRole());
    }

    @Test
    @DisplayName("Module 9: Customer Data Isolation (Cannot inspect other customer orders or tracking)")
    void testCustomerDataIsolation() {
        // Customer 1 places order
        cartService.addToCart(customer.getEmail(), new AddToCartRequest(foodItemId, 1));
        OrderResponse order1 = orderService.placeOrder(customer.getEmail(), new PlaceOrderRequest(
                "Address 1", "City", "State", "380001", "1234567890", null
        ));

        // Customer 2 attempts to view Customer 1's order details or tracking -> must throw ResourceNotFoundException for privacy
        assertThrows(ResourceNotFoundException.class, () ->
                orderService.getCustomerOrderDetails(customer2.getEmail(), order1.getId())
        );
        assertThrows(ResourceNotFoundException.class, () ->
                orderService.getOrderTracking(customer2.getEmail(), order1.getId())
        );
        assertThrows(ResourceNotFoundException.class, () ->
                orderService.cancelCustomerOrder(customer2.getEmail(), order1.getId(), new CancelOrderRequest("Malicious cancel"))
        );
    }

    @Test
    @DisplayName("Module 9: Restaurant Owner Data Isolation (Cannot manage orders belonging to another restaurant)")
    void testOwnerDataIsolation() {
        // Customer places order with Owner 1's restaurant
        cartService.addToCart(customer.getEmail(), new AddToCartRequest(foodItemId, 1));
        OrderResponse order = orderService.placeOrder(customer.getEmail(), new PlaceOrderRequest(
                "Address 1", "City", "State", "380001", "1234567890", null
        ));

        // Owner 2 attempts to accept or reject Owner 1's order -> must throw AccessDeniedException
        assertThrows(AccessDeniedException.class, () ->
                orderService.acceptOrder(owner2.getEmail(), order.getId())
        );
        assertThrows(AccessDeniedException.class, () ->
                orderService.rejectOrder(owner2.getEmail(), order.getId(), new RejectOrderRequest("Not my restaurant"))
        );
        assertThrows(AccessDeniedException.class, () ->
                orderService.getOwnerOrderDetails(owner2.getEmail(), order.getId())
        );
    }

    @Test
    @DisplayName("Module 9: Financial Integrity & Reconciliation Exposure on Post-Cancellation Payment")
    void testFinancialExposureIntegrity() {
        cartService.addToCart(customer.getEmail(), new AddToCartRequest(foodItemId, 1));
        OrderResponse order = orderService.placeOrder(customer.getEmail(), new PlaceOrderRequest(
                "Address 1", "City", "State", "380001", "1234567890", null
        ));

        // Customer cancels order before payment
        orderService.cancelCustomerOrder(customer.getEmail(), order.getId(), new CancelOrderRequest("Changed mind"));
        Order cancelledOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getOrderStatus());

        // A payment webhook arrives indicating payment was captured
        Payment payment = new Payment(cancelledOrder, "rzp_ord_post_cancel", new BigDecimal("442.50"), "INR");
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setRazorpayPaymentId("pay_post_cancel_captured");
        paymentRepository.save(payment);

        // Verify Admin Dashboard Analytics segregates this captured payment into reconciliationExposure
        var overview = adminDashboardService.getOverview();
        assertNotNull(overview);
        assertTrue(overview.getReconciliationExposure().compareTo(BigDecimal.ZERO) > 0);

        // Cancelled order must remain CANCELLED
        Order recheck = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, recheck.getOrderStatus());
    }
}
