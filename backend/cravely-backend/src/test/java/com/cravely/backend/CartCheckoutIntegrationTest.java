package com.cravely.backend;

import com.cravely.backend.dto.*;
import com.cravely.backend.entity.OrderStatus;
import com.cravely.backend.entity.PaymentStatus;
import com.cravely.backend.entity.Role;
import com.cravely.backend.entity.User;
import com.cravely.backend.exception.CartConflictException;
import com.cravely.backend.repository.UserRepository;
import com.cravely.backend.service.AuthService;
import com.cravely.backend.service.CartService;
import com.cravely.backend.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CartCheckoutIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private String customerEmail;
    private String otherCustomerEmail;

    @BeforeEach
    void setup() {
        customerEmail = "cartcustomer" + System.currentTimeMillis() + "@example.com";
        otherCustomerEmail = "othercustomer" + System.currentTimeMillis() + "@example.com";

        authService.register(new RegisterRequest("Cart Customer", customerEmail, "9876543210", "Password123!", Role.CUSTOMER));
        authService.register(new RegisterRequest("Other Customer", otherCustomerEmail, "9876543211", "Password123!", Role.CUSTOMER));
    }

    @Test
    @DisplayName("1. Synchronizing frontend cart with Pizza Theory items (f4 Paneer Tikka Pizza, f5 Margherita Classica)")
    void testSyncFrontendPizzaTheoryCart() {
        SyncCartRequest syncRequest = new SyncCartRequest(List.of(
                new SyncCartItemRequest(null, "f4", "Paneer Tikka Pizza", "pizza-theory", 1),
                new SyncCartItemRequest(null, "f5", "Margherita Classica", "pizza-theory", 1)
        ));

        CartResponse syncedCart = cartService.syncCart(customerEmail, syncRequest);

        assertNotNull(syncedCart);
        assertNotNull(syncedCart.getRestaurant());
        assertEquals("Pizza Theory", syncedCart.getRestaurant().getName());
        assertEquals(2, syncedCart.getItems().size());
        assertEquals(2, syncedCart.getTotalItems());

        // Paneer Tikka Pizza is 279.00, Margherita Classica is 229.00 -> Subtotal = 508.00
        assertEquals(new BigDecimal("508.00"), syncedCart.getSubtotal());

        // 2. Fetch persisted cart items
        CartResponse fetchedCart = cartService.getCart(customerEmail);
        assertEquals(2, fetchedCart.getItems().size());
        assertEquals(new BigDecimal("508.00"), fetchedCart.getSubtotal());
    }

    @Test
    @DisplayName("2. Successfully place order from synchronized Pizza Theory cart with coupon WELCOME10")
    void testPlaceOrderWithCoupon() {
        // Synchronize cart with 2 pizzas
        SyncCartRequest syncRequest = new SyncCartRequest(List.of(
                new SyncCartItemRequest(null, "f4", "Paneer Tikka Pizza", "pizza-theory", 1),
                new SyncCartItemRequest(null, "f5", "Margherita Classica", "pizza-theory", 1)
        ));
        cartService.syncCart(customerEmail, syncRequest);

        // Place order with WELCOME10 coupon
        PlaceOrderRequest orderRequest = new PlaceOrderRequest(
                "123 MG Road",
                "Ahmedabad",
                "Gujarat",
                "380006",
                "9876543210",
                "Leave at door",
                "WELCOME10"
        );

        OrderResponse placedOrder = orderService.placeOrder(customerEmail, orderRequest);

        assertNotNull(placedOrder);
        assertNotNull(placedOrder.getId());
        assertEquals("Pizza Theory", placedOrder.getRestaurantName());
        assertEquals(OrderStatus.PLACED, placedOrder.getOrderStatus());
        assertEquals(PaymentStatus.PENDING, placedOrder.getPaymentStatus());
        assertEquals(2, placedOrder.getItems().size());

        // Subtotal = 508.00
        assertEquals(new BigDecimal("508.00"), placedOrder.getSubtotal());
        // Discount 10% = 50.80
        assertEquals(new BigDecimal("50.80"), placedOrder.getDiscountAmount());
        // Total = 508.00 - 50.80 = 457.20
        assertEquals(new BigDecimal("457.20"), placedOrder.getTotalAmount());

        // Verify backend cart was cleared transactionally after placing order
        CartResponse postOrderCart = cartService.getCart(customerEmail);
        assertTrue(postOrderCart.getItems().isEmpty());
    }

    @Test
    @DisplayName("3. Reject placing order when cart is genuinely empty")
    void testRejectEmptyCartOrderPlacement() {
        // Customer cart has not been populated
        PlaceOrderRequest orderRequest = new PlaceOrderRequest(
                "123 MG Road",
                "Ahmedabad",
                "Gujarat",
                "380006",
                "9876543210",
                "Leave at door"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                orderService.placeOrder(customerEmail, orderRequest)
        );

        assertEquals("Cannot place order with an empty cart", ex.getMessage());
    }

    @Test
    @DisplayName("4. Cart synchronization clears previous items and sets new items cleanly")
    void testCartSyncReplacesPreviousItems() {
        // Sync 1 item
        cartService.syncCart(customerEmail, new SyncCartRequest(List.of(
                new SyncCartItemRequest(null, "f4", "Paneer Tikka Pizza", "pizza-theory", 1)
        )));
        assertEquals(1, cartService.getCart(customerEmail).getItems().size());

        // Sync different quantity or set
        cartService.syncCart(customerEmail, new SyncCartRequest(List.of(
                new SyncCartItemRequest(null, "f5", "Margherita Classica", "pizza-theory", 2)
        )));
        CartResponse updated = cartService.getCart(customerEmail);
        assertEquals(1, updated.getItems().size());
        assertEquals("Margherita Classica", updated.getItems().get(0).getFoodName());
        assertEquals(2, updated.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("458.00"), updated.getSubtotal());
    }

    @Test
    @DisplayName("5. Customer cart isolation: other customers cannot inspect or modify another's cart")
    void testCustomerCartIsolation() {
        cartService.syncCart(customerEmail, new SyncCartRequest(List.of(
                new SyncCartItemRequest(null, "f4", "Paneer Tikka Pizza", "pizza-theory", 1)
        )));

        // Other customer cart is empty
        CartResponse otherCart = cartService.getCart(otherCustomerEmail);
        assertTrue(otherCart.getItems().isEmpty());
    }
}

