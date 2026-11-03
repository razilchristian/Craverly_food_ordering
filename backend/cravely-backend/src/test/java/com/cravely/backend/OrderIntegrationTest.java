package com.cravely.backend;

import com.cravely.backend.dto.*;
import com.cravely.backend.entity.FoodType;
import com.cravely.backend.entity.OrderStatus;
import com.cravely.backend.entity.PaymentStatus;
import com.cravely.backend.entity.Role;
import com.cravely.backend.exception.ResourceNotFoundException;
import com.cravely.backend.service.*;
import com.cravely.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderIntegrationTest {

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
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private String customer1Email;
    private String customer2Email;
    private String owner1Email;
    private String owner2Email;
    private String adminEmail;

    private Long restaurant1Id;
    private Long restaurant2Id;
    private Long categoryId;

    private Long item1Id;
    private Long item2Id;
    private Long itemUnavailableId;

    @BeforeEach
    void setup() {
        long ts = System.currentTimeMillis();
        customer1Email = "ord_cust1_" + ts + "@example.com";
        customer2Email = "ord_cust2_" + ts + "@example.com";
        owner1Email = "ord_own1_" + ts + "@example.com";
        owner2Email = "ord_own2_" + ts + "@example.com";
        adminEmail = "ord_admin_" + ts + "@example.com";

        authService.register(new RegisterRequest("Order Customer 1", customer1Email, "1112223331", "Password123!", Role.CUSTOMER));
        authService.register(new RegisterRequest("Order Customer 2", customer2Email, "1112223332", "Password123!", Role.CUSTOMER));
        authService.register(new RegisterRequest("Order Owner 1", owner1Email, "1112223333", "Password123!", Role.RESTAURANT_OWNER));
        authService.register(new RegisterRequest("Order Owner 2", owner2Email, "1112223334", "Password123!", Role.RESTAURANT_OWNER));
        userRepository.save(new com.cravely.backend.entity.User("Order Admin", adminEmail, "1112223335", passwordEncoder.encode("Password123!"), Role.ADMIN, true));

        // Restaurant 1
        var r1 = restaurantService.createRestaurant(owner1Email, new RestaurantCreateRequest(
                "Bella Italia", "Authentic Italian", "10 Market St", "Ahmedabad", "Gujarat", "380001",
                "9876543201", "bella@italia.com", "Italian", null, LocalTime.of(10, 0), LocalTime.of(23, 0)
        ));
        restaurantService.approveRestaurant(r1.getId());
        restaurant1Id = r1.getId();

        // Restaurant 2 (for Owner 2)
        var r2 = restaurantService.createRestaurant(owner2Email, new RestaurantCreateRequest(
                "Kyoto Sushi", "Japanese", "20 Market St", "Ahmedabad", "Gujarat", "380001",
                "9876543202", "kyoto@sushi.com", "Japanese", null, LocalTime.of(11, 0), LocalTime.of(22, 0)
        ));
        restaurantService.approveRestaurant(r2.getId());
        restaurant2Id = r2.getId();

        // Categories & Items for Restaurant 1
        var cat = categoryService.createCategory(owner1Email, restaurant1Id, new FoodCategoryCreateRequest("Pizza", "Pizzas"));
        categoryId = cat.getId();
        var item1 = foodItemService.createFoodItem(owner1Email, restaurant1Id, new FoodItemCreateRequest(
                "Margherita", "Cheese pizza", categoryId, new BigDecimal("250.00"), null, FoodType.VEG, true, 20
        ));
        item1Id = item1.getId();

        var item2 = foodItemService.createFoodItem(owner1Email, restaurant1Id, new FoodItemCreateRequest(
                "Pollo Pizza", "Chicken pizza", categoryId, new BigDecimal("350.00"), null, FoodType.NON_VEG, true, 25
        ));
        item2Id = item2.getId();

        var unavail = foodItemService.createFoodItem(owner1Email, restaurant1Id, new FoodItemCreateRequest(
                "Truffle Pizza", "Seasonal truffle", categoryId, new BigDecimal("800.00"), null, FoodType.VEG, false, 30
        ));
        itemUnavailableId = unavail.getId();
    }

    private PlaceOrderRequest createValidPlaceOrderRequest() {
        return new PlaceOrderRequest(
                "123 Test Street",
                "Ahmedabad",
                "Gujarat",
                "380001",
                "9876543210",
                "Please ring the bell"
        );
    }

    // 1. Order Placement & Snapshotting Tests

    @Test
    void testPlaceOrderSuccess_CartClearedAndOrderCreated() {
        cartService.addToCart(customer1Email, new AddToCartRequest(item1Id, 2));
        cartService.addToCart(customer1Email, new AddToCartRequest(item2Id, 1));

        OrderResponse order = orderService.placeOrder(customer1Email, createValidPlaceOrderRequest());

        assertNotNull(order);
        assertNotNull(order.getId());
        assertTrue(order.getOrderNumber().startsWith("CRV-"));
        assertEquals(OrderStatus.PLACED, order.getOrderStatus());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals("123 Test Street", order.getDeliveryAddress());
        assertEquals("Ahmedabad", order.getDeliveryCity());
        assertEquals("9876543210", order.getCustomerPhone());
        assertEquals(2, order.getItems().size());

        // 2 * 250.00 + 1 * 350.00 = 850.00
        assertEquals(new BigDecimal("850.00"), order.getSubtotal());
        assertEquals(new BigDecimal("850.00"), order.getTotalAmount());

        // Verify cart is cleared
        CartResponse cart = cartService.getCart(customer1Email);
        assertTrue(cart.getItems().isEmpty());
        assertNull(cart.getRestaurant());
    }

    @Test
    void testHistoricalPriceSnapshot_DoesNotChangeWhenMenuItemPriceUpdates() {
        cartService.addToCart(customer1Email, new AddToCartRequest(item1Id, 1));
        OrderResponse order = orderService.placeOrder(customer1Email, createValidPlaceOrderRequest());

        assertEquals(new BigDecimal("250.00"), order.getSubtotal());

        // Update live food item price from 250.00 to 500.00
        foodItemService.updateFoodItem(owner1Email, item1Id, new FoodItemUpdateRequest(
                "Margherita", "Updated description", categoryId, new BigDecimal("500.00"), null, FoodType.VEG, true, 20
        ));

        // Refetch order details and verify snapshot price is still 250.00
        OrderResponse orderAfterPriceUpdate = orderService.getCustomerOrderDetails(customer1Email, order.getId());
        assertEquals(new BigDecimal("250.00"), orderAfterPriceUpdate.getSubtotal());
        assertEquals(new BigDecimal("250.00"), orderAfterPriceUpdate.getItems().get(0).getUnitPrice());
    }

    @Test
    void testPlaceOrder_EmptyCart_ThrowsException() {
        assertThrows(IllegalStateException.class, () ->
                orderService.placeOrder(customer1Email, createValidPlaceOrderRequest())
        );
    }

    @Test
    void testPlaceOrder_UnavailableItemInCart_ThrowsExceptionAndCartNotCleared() {
        cartService.addToCart(customer1Email, new AddToCartRequest(item1Id, 1));

        // Make item unavailable after adding to cart
        foodItemService.updateFoodItem(owner1Email, item1Id, new FoodItemUpdateRequest(
                "Margherita", "desc", categoryId, new BigDecimal("250.00"), null, FoodType.VEG, false, 20
        ));

        assertThrows(IllegalStateException.class, () ->
                orderService.placeOrder(customer1Email, createValidPlaceOrderRequest())
        );

        // Verify cart is NOT cleared
        CartResponse cart = cartService.getCart(customer1Email);
        assertEquals(1, cart.getItems().size());
    }

    @Test
    void testPlaceOrder_InactiveRestaurant_ThrowsException() {
        cartService.addToCart(customer1Email, new AddToCartRequest(item1Id, 1));

        // Deactivate restaurant
        restaurantService.deactivateRestaurantByOwner(owner1Email, restaurant1Id);

        assertThrows(IllegalStateException.class, () ->
                orderService.placeOrder(customer1Email, createValidPlaceOrderRequest())
        );
    }

    // 2. Customer Order Access & Cancellation Tests

    @Test
    void testGetMyOrdersAndOrderDetails() {
        cartService.addToCart(customer1Email, new AddToCartRequest(item1Id, 1));
        OrderResponse created = orderService.placeOrder(customer1Email, createValidPlaceOrderRequest());

        var myOrders = orderService.getMyOrders(customer1Email, null, null, null, PageRequest.of(0, 10));
        assertEquals(1, myOrders.getTotalElements());
        assertEquals(created.getOrderNumber(), myOrders.getContent().get(0).getOrderNumber());

        OrderResponse details = orderService.getCustomerOrderDetails(customer1Email, created.getId());
        assertEquals(created.getOrderNumber(), details.getOrderNumber());
    }

    @Test
    void testCustomerCannotAccessAnotherCustomersOrder() {
        cartService.addToCart(customer1Email, new AddToCartRequest(item1Id, 1));
        OrderResponse order1 = orderService.placeOrder(customer1Email, createValidPlaceOrderRequest());

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.getCustomerOrderDetails(customer2Email, order1.getId())
        );
    }

    @Test
    void testCustomerCancelPlacedOrder_Success() {
        cartService.addToCart(customer1Email, new AddToCartRequest(item1Id, 1));
        OrderResponse order = orderService.placeOrder(customer1Email, createValidPlaceOrderRequest());

        OrderResponse cancelled = orderService.cancelCustomerOrder(customer1Email, order.getId(), new CancelOrderRequest("Changed my mind"));
        assertEquals(OrderStatus.CANCELLED, cancelled.getOrderStatus());
        assertEquals("Changed my mind", cancelled.getCancellationReason());
    }

    @Test
    void testCustomerCancelAcceptedOrder_Success() {
        cartService.addToCart(customer1Email, new AddToCartRequest(item1Id, 1));
        OrderResponse order = orderService.placeOrder(customer1Email, createValidPlaceOrderRequest());

        orderService.acceptOrder(owner1Email, order.getId());

        OrderResponse cancelled = orderService.cancelCustomerOrder(customer1Email, order.getId(), new CancelOrderRequest("Found better alternative"));
        assertEquals(OrderStatus.CANCELLED, cancelled.getOrderStatus());
    }

    @Test
    void testCustomerCannotCancelPreparingOrder() {
        cartService.addToCart(customer1Email, new AddToCartRequest(item1Id, 1));
        OrderResponse order = orderService.placeOrder(customer1Email, createValidPlaceOrderRequest());

        orderService.acceptOrder(owner1Email, order.getId());
        orderService.markOrderPreparing(owner1Email, order.getId());

        assertThrows(IllegalStateException.class, () ->
                orderService.cancelCustomerOrder(customer1Email, order.getId(), new CancelOrderRequest("Too late"))
        );
    }

    @Test
    void testCustomerCannotCancelAnotherCustomersOrder() {
        cartService.addToCart(customer1Email, new AddToCartRequest(item1Id, 1));
        OrderResponse order = orderService.placeOrder(customer1Email, createValidPlaceOrderRequest());

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.cancelCustomerOrder(customer2Email, order.getId(), new CancelOrderRequest("Not my order"))
        );
    }

    // 3. Restaurant Owner Order Lifecycle & Isolation Tests

    @Test
    void testOrderLifecycle_Placed_Accepted_Preparing_Ready() {
        cartService.addToCart(customer1Email, new AddToCartRequest(item1Id, 1));
        OrderResponse order = orderService.placeOrder(customer1Email, createValidPlaceOrderRequest());

        assertEquals(OrderStatus.PLACED, order.getOrderStatus());

        OrderResponse accepted = orderService.acceptOrder(owner1Email, order.getId());
        assertEquals(OrderStatus.ACCEPTED, accepted.getOrderStatus());

        OrderResponse preparing = orderService.markOrderPreparing(owner1Email, order.getId());
        assertEquals(OrderStatus.PREPARING, preparing.getOrderStatus());

        OrderResponse ready = orderService.markOrderReady(owner1Email, order.getId());
        assertEquals(OrderStatus.READY_FOR_PICKUP, ready.getOrderStatus());
    }

    @Test
    void testOwnerRejectPlacedOrder_Success() {
        cartService.addToCart(customer1Email, new AddToCartRequest(item1Id, 1));
        OrderResponse order = orderService.placeOrder(customer1Email, createValidPlaceOrderRequest());

        OrderResponse rejected = orderService.rejectOrder(owner1Email, order.getId(), new RejectOrderRequest("Kitchen closed for deep cleaning"));
        assertEquals(OrderStatus.REJECTED, rejected.getOrderStatus());
        assertEquals("Kitchen closed for deep cleaning", rejected.getRejectionReason());
    }

    @Test
    void testOwnerCannotAccessAnotherOwnersOrders() {
        cartService.addToCart(customer1Email, new AddToCartRequest(item1Id, 1));
        OrderResponse order = orderService.placeOrder(customer1Email, createValidPlaceOrderRequest());

        // Owner 2 tries to view / accept order from Restaurant 1
        assertThrows(AccessDeniedException.class, () ->
                orderService.getOwnerOrderDetails(owner2Email, order.getId())
        );

        assertThrows(AccessDeniedException.class, () ->
                orderService.acceptOrder(owner2Email, order.getId())
        );
    }

    @Test
    void testInvalidStatusTransitions_Rejected() {
        cartService.addToCart(customer1Email, new AddToCartRequest(item1Id, 1));
        OrderResponse order = orderService.placeOrder(customer1Email, createValidPlaceOrderRequest());

        // Cannot jump directly from PLACED to PREPARING
        assertThrows(IllegalStateException.class, () ->
                orderService.markOrderPreparing(owner1Email, order.getId())
        );

        // Cannot jump directly from PLACED to READY_FOR_PICKUP
        assertThrows(IllegalStateException.class, () ->
                orderService.markOrderReady(owner1Email, order.getId())
        );

        orderService.acceptOrder(owner1Email, order.getId());

        // Cannot accept already ACCEPTED order
        assertThrows(IllegalStateException.class, () ->
                orderService.acceptOrder(owner1Email, order.getId())
        );

        // Cannot reject already ACCEPTED order
        assertThrows(IllegalStateException.class, () ->
                orderService.rejectOrder(owner1Email, order.getId(), new RejectOrderRequest("Too late"))
        );
    }

    // 4. Admin Order Management Tests

    @Test
    void testAdminViewSearchAndFilterOrders() {
        cartService.addToCart(customer1Email, new AddToCartRequest(item1Id, 1));
        OrderResponse o1 = orderService.placeOrder(customer1Email, createValidPlaceOrderRequest());

        var allOrders = orderService.getAllOrders(null, null, null, null, PageRequest.of(0, 10));
        assertTrue(allOrders.getTotalElements() >= 1);

        var byStatus = orderService.getAllOrders(OrderStatus.PLACED, null, null, null, PageRequest.of(0, 10));
        assertTrue(byStatus.getContent().stream().anyMatch(o -> o.getId().equals(o1.getId())));

        var byNumber = orderService.getAllOrders(null, o1.getOrderNumber(), null, null, PageRequest.of(0, 10));
        assertEquals(1, byNumber.getTotalElements());

        OrderResponse adminDetails = orderService.getAdminOrderDetails(o1.getId());
        assertEquals(o1.getOrderNumber(), adminDetails.getOrderNumber());
    }
}
