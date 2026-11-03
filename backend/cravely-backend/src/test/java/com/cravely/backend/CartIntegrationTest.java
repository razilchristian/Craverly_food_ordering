package com.cravely.backend;

import com.cravely.backend.dto.*;
import com.cravely.backend.entity.FoodType;
import com.cravely.backend.entity.Role;
import com.cravely.backend.exception.CartConflictException;
import com.cravely.backend.exception.ResourceNotFoundException;
import com.cravely.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CartIntegrationTest {

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

    private String customer1Email;
    private String customer2Email;
    private String ownerEmail;

    private Long restaurantAId;
    private Long restaurantBId;
    private Long unapprovedRestaurantId;

    private Long itemA1Id;
    private Long itemA2Id;
    private Long itemB1Id;
    private Long unavailableItemId;

    @BeforeEach
    void setup() {
        long ts = System.currentTimeMillis();
        customer1Email = "cart_cust1_" + ts + "@example.com";
        customer2Email = "cart_cust2_" + ts + "@example.com";
        ownerEmail = "cart_owner_" + ts + "@example.com";

        authService.register(new RegisterRequest("Customer One", customer1Email, "1110001111", "Password123!", Role.CUSTOMER));
        authService.register(new RegisterRequest("Customer Two", customer2Email, "1110001112", "Password123!", Role.CUSTOMER));
        authService.register(new RegisterRequest("Owner One", ownerEmail, "1110001113", "Password123!", Role.RESTAURANT_OWNER));

        // Create & approve Restaurant A
        RestaurantCreateRequest rA = new RestaurantCreateRequest(
                "Pizzeria Roma", "Italian Pizzas", "12 Roma Way", "Mumbai", "Maharashtra", "400001",
                "9876543210", "roma@cravely.com", "Italian", null, LocalTime.of(10, 0), LocalTime.of(23, 0)
        );
        var createdRA = restaurantService.createRestaurant(ownerEmail, rA);
        restaurantService.approveRestaurant(createdRA.getId());
        restaurantAId = createdRA.getId();

        // Create & approve Restaurant B
        RestaurantCreateRequest rB = new RestaurantCreateRequest(
                "Tokyo Sushi", "Japanese Sushi Bar", "14 Tokyo Way", "Mumbai", "Maharashtra", "400001",
                "9876543211", "tokyo@cravely.com", "Japanese", null, LocalTime.of(11, 0), LocalTime.of(22, 0)
        );
        var createdRB = restaurantService.createRestaurant(ownerEmail, rB);
        restaurantService.approveRestaurant(createdRB.getId());
        restaurantBId = createdRB.getId();

        // Create unapproved Restaurant
        RestaurantCreateRequest rUnapp = new RestaurantCreateRequest(
                "Pending Diner", "American", "15 Diner Way", "Mumbai", "Maharashtra", "400001",
                "9876543212", "pending@diner.com", "American", null, LocalTime.of(9, 0), LocalTime.of(21, 0)
        );
        var createdRUnapp = restaurantService.createRestaurant(ownerEmail, rUnapp);
        unapprovedRestaurantId = createdRUnapp.getId();

        // Create Category & Items for Restaurant A
        var catA = categoryService.createCategory(ownerEmail, restaurantAId, new FoodCategoryCreateRequest("Pizzas", "Wood-fired"));
        var itemA1 = foodItemService.createFoodItem(ownerEmail, restaurantAId, new FoodItemCreateRequest(
                "Margherita", "Cheese & Tomato", catA.getId(), new BigDecimal("300.00"), null, FoodType.VEG, true, 15
        ));
        itemA1Id = itemA1.getId();

        var itemA2 = foodItemService.createFoodItem(ownerEmail, restaurantAId, new FoodItemCreateRequest(
                "Pepperoni", "Spicy Pepperoni", catA.getId(), new BigDecimal("450.00"), null, FoodType.NON_VEG, true, 20
        ));
        itemA2Id = itemA2.getId();

        var unavailItem = foodItemService.createFoodItem(ownerEmail, restaurantAId, new FoodItemCreateRequest(
                "Truffle Pizza", "Seasonal", catA.getId(), new BigDecimal("600.00"), null, FoodType.VEG, false, 25
        ));
        unavailableItemId = unavailItem.getId();

        // Create Category & Item for Restaurant B
        var catB = categoryService.createCategory(ownerEmail, restaurantBId, new FoodCategoryCreateRequest("Sushi", "Fresh Rolls"));
        var itemB1 = foodItemService.createFoodItem(ownerEmail, restaurantBId, new FoodItemCreateRequest(
                "Salmon Roll", "Fresh Atlantic Salmon", catB.getId(), new BigDecimal("550.00"), null, FoodType.NON_VEG, true, 15
        ));
        itemB1Id = itemB1.getId();
    }

    @Test
    void testAddToCartAndSubtotalCalculation() {
        // Add 2 Margheritas (300.00 each)
        CartResponse cart = cartService.addToCart(customer1Email, new AddToCartRequest(itemA1Id, 2));

        assertNotNull(cart.getCartId());
        assertEquals(restaurantAId, cart.getRestaurant().getId());
        assertEquals("Pizzeria Roma", cart.getRestaurant().getName());
        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getTotalItems());
        assertEquals(new BigDecimal("600.00"), cart.getSubtotal());

        // Add 1 Pepperoni (450.00)
        cart = cartService.addToCart(customer1Email, new AddToCartRequest(itemA2Id, 1));
        assertEquals(2, cart.getItems().size());
        assertEquals(3, cart.getTotalItems());
        assertEquals(new BigDecimal("1050.00"), cart.getSubtotal());
    }

    @Test
    void testAddingSameItemIncreasesQuantity() {
        cartService.addToCart(customer1Email, new AddToCartRequest(itemA1Id, 2));
        CartResponse cart = cartService.addToCart(customer1Email, new AddToCartRequest(itemA1Id, 3));

        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getTotalItems());
        assertEquals(5, cart.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("1500.00"), cart.getSubtotal());
    }

    @Test
    void testUpdateCartItemQuantityAndValidation() {
        CartResponse cart = cartService.addToCart(customer1Email, new AddToCartRequest(itemA1Id, 2));
        Long cartItemId = cart.getItems().get(0).getCartItemId();

        // Increase quantity to 4
        CartResponse updated = cartService.updateCartItemQuantity(customer1Email, cartItemId, new UpdateCartItemRequest(4));
        assertEquals(4, updated.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("1200.00"), updated.getSubtotal());

        // Decrease quantity to 1
        updated = cartService.updateCartItemQuantity(customer1Email, cartItemId, new UpdateCartItemRequest(1));
        assertEquals(1, updated.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("300.00"), updated.getSubtotal());

        // Zero quantity rejection
        assertThrows(IllegalArgumentException.class, () ->
                cartService.updateCartItemQuantity(customer1Email, cartItemId, new UpdateCartItemRequest(0))
        );

        // Negative quantity rejection
        assertThrows(IllegalArgumentException.class, () ->
                cartService.updateCartItemQuantity(customer1Email, cartItemId, new UpdateCartItemRequest(-2))
        );
    }

    @Test
    void testRemoveCartItemAndClearCart() {
        cartService.addToCart(customer1Email, new AddToCartRequest(itemA1Id, 2));
        CartResponse cart = cartService.addToCart(customer1Email, new AddToCartRequest(itemA2Id, 1));
        assertEquals(2, cart.getItems().size());

        Long itemA1CartId = cart.getItems().stream()
                .filter(i -> i.getFoodItemId().equals(itemA1Id)).findFirst().orElseThrow().getCartItemId();

        // Remove single item
        CartResponse afterRemove = cartService.removeCartItem(customer1Email, itemA1CartId);
        assertEquals(1, afterRemove.getItems().size());
        assertEquals(new BigDecimal("450.00"), afterRemove.getSubtotal());

        // Clear cart
        CartResponse cleared = cartService.clearCart(customer1Email);
        assertEquals(0, cleared.getTotalItems());
        assertEquals(0, cleared.getItems().size());
        assertEquals(new BigDecimal("0.00"), cleared.getSubtotal());
    }

    @Test
    void testMultiRestaurantConflict409() {
        // Add item from Restaurant A
        cartService.addToCart(customer1Email, new AddToCartRequest(itemA1Id, 1));

        // Attempt to add item from Restaurant B -> should throw CartConflictException
        assertThrows(CartConflictException.class, () ->
                cartService.addToCart(customer1Email, new AddToCartRequest(itemB1Id, 1))
        );

        // After clearing cart, adding item from Restaurant B must succeed
        cartService.clearCart(customer1Email);
        CartResponse cartB = cartService.addToCart(customer1Email, new AddToCartRequest(itemB1Id, 1));
        assertEquals(restaurantBId, cartB.getRestaurant().getId());
        assertEquals("Tokyo Sushi", cartB.getRestaurant().getName());
        assertEquals(new BigDecimal("550.00"), cartB.getSubtotal());
    }

    @Test
    void testFoodItemValidation() {
        // Zero quantity rejection on add
        assertThrows(IllegalArgumentException.class, () ->
                cartService.addToCart(customer1Email, new AddToCartRequest(itemA1Id, 0))
        );

        // Non-existent food item
        assertThrows(ResourceNotFoundException.class, () ->
                cartService.addToCart(customer1Email, new AddToCartRequest(999999L, 1))
        );

        // Unavailable food item rejection
        assertThrows(IllegalStateException.class, () ->
                cartService.addToCart(customer1Email, new AddToCartRequest(unavailableItemId, 1))
        );
    }

    @Test
    void testCrossCustomerAccessBlocked() {
        CartResponse cart1 = cartService.addToCart(customer1Email, new AddToCartRequest(itemA1Id, 2));
        Long cartItemId = cart1.getItems().get(0).getCartItemId();

        // Customer 2 attempts to modify Customer 1's cart item -> 404/Access Denied
        assertThrows(ResourceNotFoundException.class, () ->
                cartService.updateCartItemQuantity(customer2Email, cartItemId, new UpdateCartItemRequest(5))
        );

        assertThrows(ResourceNotFoundException.class, () ->
                cartService.removeCartItem(customer2Email, cartItemId)
        );
    }

    @Test
    void testNonCustomerRoleBlockedFromCart() {
        // Owner attempting cart operations -> AccessDeniedException
        assertThrows(AccessDeniedException.class, () ->
                cartService.addToCart(ownerEmail, new AddToCartRequest(itemA1Id, 1))
        );

        assertThrows(AccessDeniedException.class, () ->
                cartService.getCart(ownerEmail)
        );
    }
}

