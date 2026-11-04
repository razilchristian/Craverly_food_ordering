package com.cravely.backend;

import com.cravely.backend.controller.AdminOrderController;
import com.cravely.backend.controller.OrderController;
import com.cravely.backend.controller.OwnerOrderController;
import com.cravely.backend.dto.*;
import com.cravely.backend.entity.*;
import com.cravely.backend.exception.ResourceNotFoundException;
import com.cravely.backend.repository.OrderStatusHistoryRepository;
import com.cravely.backend.repository.UserRepository;
import com.cravely.backend.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderTrackingIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderController orderController;

    @Autowired
    private OwnerOrderController ownerOrderController;

    @Autowired
    private AdminOrderController adminOrderController;

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
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OrderStatusHistoryRepository historyRepository;

    private User customer1;
    private User customer2;
    private User owner1;
    private User owner2;
    private User admin;

    private Long restaurant1Id;
    private Long restaurant2Id;
    private Long item1Id;
    private Long item2Id;

    @BeforeEach
    void setup() {
        long ts = System.currentTimeMillis();
        String c1Email = "trk_cust1_" + ts + "@example.com";
        String c2Email = "trk_cust2_" + ts + "@example.com";
        String o1Email = "trk_own1_" + ts + "@example.com";
        String o2Email = "trk_own2_" + ts + "@example.com";
        String aEmail = "trk_admin_" + ts + "@example.com";

        authService.register(new RegisterRequest("Tracking Customer 1", c1Email, "1112223341", "Password123!", Role.CUSTOMER));
        authService.register(new RegisterRequest("Tracking Customer 2", c2Email, "1112223342", "Password123!", Role.CUSTOMER));
        authService.register(new RegisterRequest("Tracking Owner 1", o1Email, "1112223343", "Password123!", Role.RESTAURANT_OWNER));
        authService.register(new RegisterRequest("Tracking Owner 2", o2Email, "1112223344", "Password123!", Role.RESTAURANT_OWNER));
        userRepository.save(new User("Tracking Admin", aEmail, "1112223345", passwordEncoder.encode("Password123!"), Role.ADMIN, true));

        customer1 = userRepository.findByEmail(c1Email).orElseThrow();
        customer2 = userRepository.findByEmail(c2Email).orElseThrow();
        owner1 = userRepository.findByEmail(o1Email).orElseThrow();
        owner2 = userRepository.findByEmail(o2Email).orElseThrow();
        admin = userRepository.findByEmail(aEmail).orElseThrow();

        // Restaurant 1
        var r1 = restaurantService.createRestaurant(owner1.getEmail(), new RestaurantCreateRequest(
                "Trk Bistro", "French Cuisine", "10 Rue Paris", "Ahmedabad", "Gujarat", "380001",
                "9876543201", "bistro@trk.com", "French", null, LocalTime.of(10, 0), LocalTime.of(23, 0)
        ));
        restaurantService.approveRestaurant(r1.getId());
        restaurant1Id = r1.getId();

        // Restaurant 2
        var r2 = restaurantService.createRestaurant(owner2.getEmail(), new RestaurantCreateRequest(
                "Trk Diner", "American Cuisine", "20 Broadway", "Ahmedabad", "Gujarat", "380001",
                "9876543202", "diner@trk.com", "American", null, LocalTime.of(10, 0), LocalTime.of(23, 0)
        ));
        restaurantService.approveRestaurant(r2.getId());
        restaurant2Id = r2.getId();

        // Categories & Items
        var cat = categoryService.createCategory(owner1.getEmail(), restaurant1Id, new FoodCategoryCreateRequest("Mains", "Main dishes"));
        var item1 = foodItemService.createFoodItem(owner1.getEmail(), restaurant1Id, new FoodItemCreateRequest(
                "Croissant Burger", "Flaky burger", cat.getId(), new BigDecimal("250.00"), null, FoodType.VEG, true, 20
        ));
        item1Id = item1.getId();

        var cat2 = categoryService.createCategory(owner2.getEmail(), restaurant2Id, new FoodCategoryCreateRequest("Burgers", "Burgers"));
        var item2 = foodItemService.createFoodItem(owner2.getEmail(), restaurant2Id, new FoodItemCreateRequest(
                "Classic Burger", "Beef burger", cat2.getId(), new BigDecimal("300.00"), null, FoodType.NON_VEG, true, 20
        ));
        item2Id = item2.getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Principal principalOf(String email) {
        return () -> email;
    }

    private PlaceOrderRequest createValidPlaceOrderRequest() {
        return new PlaceOrderRequest("100 Track Road", "Ahmedabad", "Gujarat", "380001", "9876543210", "Leave at front desk");
    }

    private OrderResponse placeCustomer1Order() {
        cartService.addToCart(customer1.getEmail(), new AddToCartRequest(item1Id, 2));
        return orderService.placeOrder(customer1.getEmail(), createValidPlaceOrderRequest());
    }

    @Test
    void test01_InitialPlacedHistoryGeneratedOnOrderPlacement() {
        OrderResponse order = placeCustomer1Order();

        List<OrderStatusHistory> history = historyRepository.findByOrderIdOrderByChangedAtAsc(order.getId());
        assertEquals(1, history.size());

        OrderStatusHistory entry = history.get(0);
        assertNull(entry.getPreviousStatus());
        assertEquals(OrderStatus.PLACED, entry.getNewStatus());
        assertNotNull(entry.getChangedBy());
        assertEquals(customer1.getId(), entry.getChangedBy().getId());
        assertEquals(Role.CUSTOMER, entry.getChangedBy().getRole());
        assertTrue(entry.getRemarks().contains("Order placed"));
        assertNotNull(entry.getChangedAt());
    }

    @Test
    void test02_OrderAcceptanceCreatesAcceptedHistoryRecord() {
        OrderResponse order = placeCustomer1Order();

        orderService.acceptOrder(owner1.getEmail(), order.getId());

        List<OrderStatusHistory> history = historyRepository.findByOrderIdOrderByChangedAtAsc(order.getId());
        assertEquals(2, history.size());

        OrderStatusHistory entry = history.get(1);
        assertEquals(OrderStatus.PLACED, entry.getPreviousStatus());
        assertEquals(OrderStatus.ACCEPTED, entry.getNewStatus());
        assertEquals(owner1.getId(), entry.getChangedBy().getId());
        assertEquals(Role.RESTAURANT_OWNER, entry.getChangedBy().getRole());
        assertTrue(entry.getRemarks().contains("accepted"));
    }

    @Test
    void test03_OrderRejectionCreatesRejectedHistoryRecord() {
        OrderResponse order = placeCustomer1Order();

        orderService.rejectOrder(owner1.getEmail(), order.getId(), new RejectOrderRequest("Kitchen closed for cleaning"));

        List<OrderStatusHistory> history = historyRepository.findByOrderIdOrderByChangedAtAsc(order.getId());
        assertEquals(2, history.size());

        OrderStatusHistory entry = history.get(1);
        assertEquals(OrderStatus.PLACED, entry.getPreviousStatus());
        assertEquals(OrderStatus.REJECTED, entry.getNewStatus());
        assertEquals(owner1.getId(), entry.getChangedBy().getId());
        assertTrue(entry.getRemarks().contains("Kitchen closed for cleaning"));
    }

    @Test
    void test04_PreparingTransitionCreatesHistoryRecord() {
        OrderResponse order = placeCustomer1Order();
        orderService.acceptOrder(owner1.getEmail(), order.getId());

        orderService.markOrderPreparing(owner1.getEmail(), order.getId());

        List<OrderStatusHistory> history = historyRepository.findByOrderIdOrderByChangedAtAsc(order.getId());
        assertEquals(3, history.size());

        OrderStatusHistory entry = history.get(2);
        assertEquals(OrderStatus.ACCEPTED, entry.getPreviousStatus());
        assertEquals(OrderStatus.PREPARING, entry.getNewStatus());
        assertEquals(owner1.getId(), entry.getChangedBy().getId());
        assertTrue(entry.getRemarks().contains("preparation"));
    }

    @Test
    void test05_ReadyForPickupTransitionCreatesHistoryRecord() {
        OrderResponse order = placeCustomer1Order();
        orderService.acceptOrder(owner1.getEmail(), order.getId());
        orderService.markOrderPreparing(owner1.getEmail(), order.getId());

        orderService.markOrderReady(owner1.getEmail(), order.getId());

        List<OrderStatusHistory> history = historyRepository.findByOrderIdOrderByChangedAtAsc(order.getId());
        assertEquals(4, history.size());

        OrderStatusHistory entry = history.get(3);
        assertEquals(OrderStatus.PREPARING, entry.getPreviousStatus());
        assertEquals(OrderStatus.READY_FOR_PICKUP, entry.getNewStatus());
        assertEquals(owner1.getId(), entry.getChangedBy().getId());
        assertTrue(entry.getRemarks().contains("ready"));
    }

    @Test
    void test06_OutForDeliveryTransitionCreatesHistoryRecord() {
        OrderResponse order = placeCustomer1Order();
        orderService.acceptOrder(owner1.getEmail(), order.getId());
        orderService.markOrderPreparing(owner1.getEmail(), order.getId());
        orderService.markOrderReady(owner1.getEmail(), order.getId());

        orderService.markOrderOutForDeliveryByOwner(owner1.getEmail(), order.getId());

        List<OrderStatusHistory> history = historyRepository.findByOrderIdOrderByChangedAtAsc(order.getId());
        assertEquals(5, history.size());

        OrderStatusHistory entry = history.get(4);
        assertEquals(OrderStatus.READY_FOR_PICKUP, entry.getPreviousStatus());
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, entry.getNewStatus());
        assertEquals(owner1.getId(), entry.getChangedBy().getId());
        assertTrue(entry.getRemarks().contains("delivery"));
    }

    @Test
    void test07_DeliveredTransitionCreatesHistoryRecord() {
        OrderResponse order = placeCustomer1Order();
        orderService.acceptOrder(owner1.getEmail(), order.getId());
        orderService.markOrderPreparing(owner1.getEmail(), order.getId());
        orderService.markOrderReady(owner1.getEmail(), order.getId());
        orderService.markOrderOutForDeliveryByOwner(owner1.getEmail(), order.getId());

        orderService.markOrderDeliveredByOwner(owner1.getEmail(), order.getId());

        List<OrderStatusHistory> history = historyRepository.findByOrderIdOrderByChangedAtAsc(order.getId());
        assertEquals(6, history.size());

        OrderStatusHistory entry = history.get(5);
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, entry.getPreviousStatus());
        assertEquals(OrderStatus.DELIVERED, entry.getNewStatus());
        assertEquals(owner1.getId(), entry.getChangedBy().getId());
        assertTrue(entry.getRemarks().contains("delivered"));
    }

    @Test
    void test08_CancellationCreatesCancelledHistoryRecord() {
        OrderResponse order = placeCustomer1Order();

        orderService.cancelCustomerOrder(customer1.getEmail(), order.getId(), new CancelOrderRequest("Delivery taking too long"));

        List<OrderStatusHistory> history = historyRepository.findByOrderIdOrderByChangedAtAsc(order.getId());
        assertEquals(2, history.size());

        OrderStatusHistory entry = history.get(1);
        assertEquals(OrderStatus.PLACED, entry.getPreviousStatus());
        assertEquals(OrderStatus.CANCELLED, entry.getNewStatus());
        assertEquals(customer1.getId(), entry.getChangedBy().getId());
        assertTrue(entry.getRemarks().contains("Delivery taking too long"));
    }

    @Test
    void test09_FailedTransitionDoesNotCreateHistory() {
        OrderResponse order = placeCustomer1Order();
        assertEquals(1, historyRepository.findByOrderIdOrderByChangedAtAsc(order.getId()).size());

        // Cannot jump directly from PLACED to PREPARING
        assertThrows(IllegalStateException.class, () ->
                orderService.markOrderPreparing(owner1.getEmail(), order.getId())
        );

        // History count remains unchanged
        assertEquals(1, historyRepository.findByOrderIdOrderByChangedAtAsc(order.getId()).size());
    }

    @Test
    void test10_RepeatedUnchangedStatusDoesNotCreateDuplicateHistory() {
        OrderResponse order = placeCustomer1Order();
        orderService.acceptOrder(owner1.getEmail(), order.getId());
        assertEquals(2, historyRepository.findByOrderIdOrderByChangedAtAsc(order.getId()).size());

        // Try to accept again
        assertThrows(IllegalStateException.class, () ->
                orderService.acceptOrder(owner1.getEmail(), order.getId())
        );

        // Count must still be 2
        assertEquals(2, historyRepository.findByOrderIdOrderByChangedAtAsc(order.getId()).size());
    }

    @Test
    void test11_StatusHistoryReturnedInChronologicalOrder() {
        OrderResponse order = placeCustomer1Order();
        orderService.acceptOrder(owner1.getEmail(), order.getId());
        orderService.markOrderPreparing(owner1.getEmail(), order.getId());
        orderService.markOrderReady(owner1.getEmail(), order.getId());
        orderService.markOrderOutForDeliveryByOwner(owner1.getEmail(), order.getId());
        orderService.markOrderDeliveredByOwner(owner1.getEmail(), order.getId());

        List<OrderStatusHistoryResponse> history = orderService.getOrderTrackingHistory(customer1.getEmail(), order.getId());
        assertEquals(6, history.size());

        assertEquals(OrderStatus.PLACED, history.get(0).getNewStatus());
        assertNull(history.get(0).getPreviousStatus());

        assertEquals(OrderStatus.ACCEPTED, history.get(1).getNewStatus());
        assertEquals(OrderStatus.PLACED, history.get(1).getPreviousStatus());

        assertEquals(OrderStatus.PREPARING, history.get(2).getNewStatus());
        assertEquals(OrderStatus.ACCEPTED, history.get(2).getPreviousStatus());

        assertEquals(OrderStatus.READY_FOR_PICKUP, history.get(3).getNewStatus());
        assertEquals(OrderStatus.PREPARING, history.get(3).getPreviousStatus());

        assertEquals(OrderStatus.OUT_FOR_DELIVERY, history.get(4).getNewStatus());
        assertEquals(OrderStatus.READY_FOR_PICKUP, history.get(4).getPreviousStatus());

        assertEquals(OrderStatus.DELIVERED, history.get(5).getNewStatus());
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, history.get(5).getPreviousStatus());

        for (int i = 0; i < history.size() - 1; i++) {
            assertFalse(history.get(i).getChangedAt().isAfter(history.get(i + 1).getChangedAt()));
        }
    }

    @Test
    void test12_ActorInformationRecordedAccurately() {
        OrderResponse order = placeCustomer1Order();
        orderService.acceptOrder(owner1.getEmail(), order.getId());
        orderService.markOrderPreparing(owner1.getEmail(), order.getId());
        orderService.markOrderReady(owner1.getEmail(), order.getId());
        orderService.markOrderOutForDeliveryByAdmin(admin.getEmail(), order.getId());

        List<OrderStatusHistoryResponse> history = orderService.getOrderTrackingHistory(customer1.getEmail(), order.getId());

        // PLACED by CUSTOMER
        assertEquals("CUSTOMER", history.get(0).getChangedByRole());
        assertEquals(customer1.getFullName(), history.get(0).getChangedBy());

        // ACCEPTED by RESTAURANT_OWNER
        assertEquals("RESTAURANT_OWNER", history.get(1).getChangedByRole());
        assertEquals(owner1.getFullName(), history.get(1).getChangedBy());

        // OUT_FOR_DELIVERY by ADMIN
        assertEquals("ADMIN", history.get(4).getChangedByRole());
        assertEquals(admin.getFullName(), history.get(4).getChangedBy());
    }

    @Test
    void test13_CustomerCanTrackTheirOwnOrder() {
        OrderResponse order = placeCustomer1Order();

        // Through service
        OrderTrackingResponse tracking = orderService.getOrderTracking(customer1.getEmail(), order.getId());
        assertNotNull(tracking);
        assertEquals(order.getId(), tracking.getOrderId());
        assertEquals(order.getOrderNumber(), tracking.getOrderNumber());
        assertEquals(OrderStatus.PLACED, tracking.getOrderStatus());
        assertEquals("Trk Bistro", tracking.getRestaurantName());
        assertEquals(1, tracking.getStatusHistory().size());
        assertEquals(1, tracking.getItems().size());

        // Through controller
        authenticateAs(customer1);
        ResponseEntity<ApiResponse<OrderTrackingResponse>> response =
                orderController.getOrderTracking(principalOf(customer1.getEmail()), order.getId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals(order.getId(), response.getBody().getData().getOrderId());
    }

    @Test
    void test14_CustomerCannotTrackAnotherCustomersOrder() {
        OrderResponse order = placeCustomer1Order();

        // Customer 2 trying to track Customer 1's order must throw ResourceNotFoundException (404)
        assertThrows(ResourceNotFoundException.class, () ->
                orderService.getOrderTracking(customer2.getEmail(), order.getId())
        );

        // Through controller
        authenticateAs(customer2);
        assertThrows(ResourceNotFoundException.class, () ->
                orderController.getOrderTracking(principalOf(customer2.getEmail()), order.getId())
        );
    }

    @Test
    void test15_CustomerCannotAccessAnotherCustomersHistory() {
        OrderResponse order = placeCustomer1Order();

        // Customer 2 trying to fetch history for Customer 1's order must throw ResourceNotFoundException (404)
        assertThrows(ResourceNotFoundException.class, () ->
                orderService.getOrderTrackingHistory(customer2.getEmail(), order.getId())
        );

        // Through controller
        authenticateAs(customer2);
        assertThrows(ResourceNotFoundException.class, () ->
                orderController.getOrderTrackingHistory(principalOf(customer2.getEmail()), order.getId())
        );
    }

    @Test
    void test16_OwnerCanTrackOrderForTheirRestaurant() {
        OrderResponse order = placeCustomer1Order();

        OrderTrackingResponse tracking = orderService.getOwnerOrderTracking(owner1.getEmail(), order.getId());
        assertNotNull(tracking);
        assertEquals(order.getId(), tracking.getOrderId());
        assertEquals("Trk Bistro", tracking.getRestaurantName());

        // Through owner controller
        authenticateAs(owner1);
        ResponseEntity<ApiResponse<OrderTrackingResponse>> response =
                ownerOrderController.getOwnerOrderTracking(principalOf(owner1.getEmail()), order.getId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals(order.getId(), response.getBody().getData().getOrderId());
    }

    @Test
    void test17_OwnerCannotTrackOrderForAnotherRestaurant() {
        OrderResponse order = placeCustomer1Order();

        // Owner 2 owns restaurant 2, order belongs to restaurant 1
        assertThrows(AccessDeniedException.class, () ->
                orderService.getOwnerOrderTracking(owner2.getEmail(), order.getId())
        );

        // Through owner controller
        authenticateAs(owner2);
        assertThrows(AccessDeniedException.class, () ->
                ownerOrderController.getOwnerOrderTracking(principalOf(owner2.getEmail()), order.getId())
        );
    }

    @Test
    void test18_AdminCanInspectTrackingForAnyOrder() {
        OrderResponse order = placeCustomer1Order();

        OrderTrackingResponse tracking = orderService.getAdminOrderTracking(order.getId());
        assertNotNull(tracking);
        assertEquals(order.getId(), tracking.getOrderId());

        // Through admin controller
        authenticateAs(admin);
        ResponseEntity<ApiResponse<OrderTrackingResponse>> response =
                adminOrderController.getAdminOrderTracking(order.getId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals(order.getId(), response.getBody().getData().getOrderId());
    }

    @Test
    void test19_RoleAndSecurityAnnotationsConfigured() {
        // Verify class-level PreAuthorize annotations enforcing role isolation
        PreAuthorize orderControllerAuth = OrderController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(orderControllerAuth);
        assertEquals("hasRole('CUSTOMER')", orderControllerAuth.value());

        PreAuthorize ownerControllerAuth = OwnerOrderController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(ownerControllerAuth);
        assertEquals("hasRole('RESTAURANT_OWNER')", ownerControllerAuth.value());

        PreAuthorize adminControllerAuth = AdminOrderController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(adminControllerAuth);
        assertEquals("hasRole('ADMIN')", adminControllerAuth.value());

        // Calling controllers without authentication must throw AuthenticationCredentialsNotFoundException
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                orderController.getOrderTracking(principalOf(customer1.getEmail()), 1L)
        );
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                ownerOrderController.getOwnerOrderTracking(principalOf(owner1.getEmail()), 1L)
        );
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                adminOrderController.getAdminOrderTracking(1L)
        );
    }
}
