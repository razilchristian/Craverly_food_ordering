package com.cravely.backend.service;

import com.cravely.backend.config.DataInitializer;
import com.cravely.backend.dto.AddToCartRequest;
import com.cravely.backend.dto.CartResponse;
import com.cravely.backend.dto.SyncCartItemRequest;
import com.cravely.backend.dto.SyncCartRequest;
import com.cravely.backend.dto.UpdateCartItemRequest;
import com.cravely.backend.entity.*;
import com.cravely.backend.exception.CartConflictException;
import com.cravely.backend.exception.ResourceNotFoundException;
import com.cravely.backend.repository.CartItemRepository;
import com.cravely.backend.repository.CartRepository;
import com.cravely.backend.repository.FoodItemRepository;
import com.cravely.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodItemRepository foodItemRepository;
    private final UserRepository userRepository;
    private final DataInitializer dataInitializer;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       FoodItemRepository foodItemRepository,
                       UserRepository userRepository,
                       @org.springframework.beans.factory.annotation.Autowired(required = false) DataInitializer dataInitializer) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.foodItemRepository = foodItemRepository;
        this.userRepository = userRepository;
        this.dataInitializer = dataInitializer;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(String customerEmail) {
        User customer = getCustomer(customerEmail);
        return cartRepository.findByUserId(customer.getId())
                .map(CartResponse::fromEntity)
                .orElseGet(CartResponse::empty);
    }

    @Transactional
    public CartResponse addToCart(String customerEmail, AddToCartRequest request) {
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        User customer = getCustomer(customerEmail);

        FoodItem foodItem = foodItemRepository.findById(request.getFoodItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + request.getFoodItemId()));

        // Validate food item availability
        if (!Boolean.TRUE.equals(foodItem.getAvailable())) {
            throw new IllegalStateException("Food item '" + foodItem.getName() + "' is currently unavailable");
        }

        // Validate food category active status
        if (foodItem.getCategory() == null || !Boolean.TRUE.equals(foodItem.getCategory().getActive())) {
            throw new IllegalStateException("Category for food item '" + foodItem.getName() + "' is currently inactive");
        }

        // Validate restaurant status and active flag
        Restaurant restaurant = foodItem.getRestaurant();
        if (restaurant == null || restaurant.getStatus() != RestaurantStatus.APPROVED || !Boolean.TRUE.equals(restaurant.getActive())) {
            throw new IllegalStateException("Restaurant for food item '" + foodItem.getName() + "' is unapproved or inactive");
        }

        // Find or create customer cart
        Cart cart = cartRepository.findByUserId(customer.getId())
                .orElseGet(() -> cartRepository.save(new Cart(customer)));

        // Multi-restaurant conflict check: Cart can contain items from only one restaurant
        if (cart.getRestaurant() != null && !cart.getItems().isEmpty()) {
            if (!cart.getRestaurant().getId().equals(restaurant.getId())) {
                throw new CartConflictException("Your cart already contains items from '" + cart.getRestaurant().getName()
                        + "'. Clear your cart before adding items from another restaurant.");
            }
        } else {
            // Assign restaurant to cart if newly created or previously empty
            cart.setRestaurant(restaurant);
        }

        // Check if item already exists in cart; if so, increase quantity instead of creating a duplicate row
        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(ci -> ci.getFoodItem().getId().equals(foodItem.getId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existing = existingItemOpt.get();
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
        } else {
            CartItem newItem = new CartItem(cart, foodItem, request.getQuantity());
            cart.addItem(newItem);
        }

        Cart saved = cartRepository.save(cart);
        return CartResponse.fromEntity(saved);
    }

    @Transactional
    public CartResponse updateCartItemQuantity(String customerEmail, Long cartItemId, UpdateCartItemRequest request) {
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1. If you wish to remove the item, please use the remove item endpoint.");
        }

        User customer = getCustomer(customerEmail);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        validateCartOwnership(cartItem.getCart(), customer);

        cartItem.setQuantity(request.getQuantity());
        Cart saved = cartRepository.save(cartItem.getCart());
        return CartResponse.fromEntity(saved);
    }

    @Transactional
    public CartResponse removeCartItem(String customerEmail, Long cartItemId) {
        User customer = getCustomer(customerEmail);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        Cart cart = cartItem.getCart();
        validateCartOwnership(cart, customer);

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        // If the cart becomes empty, reset restaurant association
        if (cart.getItems().isEmpty()) {
            cart.setRestaurant(null);
        }

        Cart saved = cartRepository.save(cart);
        return CartResponse.fromEntity(saved);
    }

    @Transactional
    public CartResponse clearCart(String customerEmail) {
        User customer = getCustomer(customerEmail);

        Optional<Cart> cartOpt = cartRepository.findByUserId(customer.getId());
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            cart.clearItems();
            cartRepository.save(cart);
        }

        return CartResponse.empty();
    }

    @Transactional
    public CartResponse syncCart(String customerEmail, SyncCartRequest request) {
        User customer = getCustomer(customerEmail);

        Cart cart = cartRepository.findByUserId(customer.getId())
                .orElseGet(() -> cartRepository.save(new Cart(customer)));

        cart.clearItems();

        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            cart.setRestaurant(null);
            Cart saved = cartRepository.save(cart);
            return CartResponse.fromEntity(saved);
        }

        for (SyncCartItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() < 1) {
                continue;
            }

            FoodItem foodItem = resolveFoodItem(itemReq);
            if (foodItem == null) {
                continue;
            }

            if (!Boolean.TRUE.equals(foodItem.getAvailable())) {
                throw new IllegalStateException("Food item '" + foodItem.getName() + "' is currently unavailable");
            }

            Restaurant restaurant = foodItem.getRestaurant();
            if (restaurant == null || restaurant.getStatus() != RestaurantStatus.APPROVED || !Boolean.TRUE.equals(restaurant.getActive())) {
                throw new IllegalStateException("Restaurant for food item '" + foodItem.getName() + "' is unapproved or inactive");
            }

            if (cart.getRestaurant() == null || cart.getItems().isEmpty()) {
                cart.setRestaurant(restaurant);
            } else if (!cart.getRestaurant().getId().equals(restaurant.getId())) {
                throw new CartConflictException("Your cart already contains items from '" + cart.getRestaurant().getName()
                        + "'. Clear your cart before adding items from another restaurant.");
            }

            Optional<CartItem> existingItemOpt = cart.getItems().stream()
                    .filter(ci -> ci.getFoodItem().getId().equals(foodItem.getId()))
                    .findFirst();

            if (existingItemOpt.isPresent()) {
                CartItem existing = existingItemOpt.get();
                existing.setQuantity(existing.getQuantity() + itemReq.getQuantity());
            } else {
                CartItem newItem = new CartItem(cart, foodItem, itemReq.getQuantity());
                cart.addItem(newItem);
            }
        }

        Cart saved = cartRepository.save(cart);
        return CartResponse.fromEntity(saved);
    }

    private FoodItem resolveFoodItem(SyncCartItemRequest itemReq) {
        // 1. By direct numeric foodItemId
        if (itemReq.getFoodItemId() != null) {
            Optional<FoodItem> byId = foodItemRepository.findById(itemReq.getFoodItemId());
            if (byId.isPresent()) {
                return byId.get();
            }
        }

        // 2. By item name
        if (itemReq.getName() != null && !itemReq.getName().isBlank()) {
            Optional<FoodItem> byName = foodItemRepository.findFirstByNameIgnoreCaseAndAvailableTrue(itemReq.getName().trim());
            if (byName.isPresent()) {
                return byName.get();
            }
        }

        // 3. By itemCode mapping (e.g. "f4" -> "Paneer Tikka Pizza", "f5" -> "Margherita Classica")
        if (itemReq.getItemCode() != null && !itemReq.getItemCode().isBlank()) {
            String mappedName = mapMockCodeToName(itemReq.getItemCode().trim());
            if (mappedName != null) {
                Optional<FoodItem> byMapped = foodItemRepository.findFirstByNameIgnoreCaseAndAvailableTrue(mappedName);
                if (byMapped.isPresent()) {
                    return byMapped.get();
                }
            }

            try {
                Long parsedId = Long.parseLong(itemReq.getItemCode().replaceAll("[^0-9]", ""));
                Optional<FoodItem> byParsed = foodItemRepository.findById(parsedId);
                if (byParsed.isPresent()) {
                    return byParsed.get();
                }
            } catch (Exception ignored) {
            }
        }

        // 4. Fallback: seed catalog if Pizza Theory hasn't been initialized yet
        if (dataInitializer != null) {
            dataInitializer.seedPizzaTheory();
            if (itemReq.getName() != null && !itemReq.getName().isBlank()) {
                Optional<FoodItem> byName = foodItemRepository.findFirstByNameIgnoreCaseAndAvailableTrue(itemReq.getName().trim());
                if (byName.isPresent()) {
                    return byName.get();
                }
            }
            if (itemReq.getItemCode() != null) {
                String mappedName = mapMockCodeToName(itemReq.getItemCode().trim());
                if (mappedName != null) {
                    Optional<FoodItem> byMapped = foodItemRepository.findFirstByNameIgnoreCaseAndAvailableTrue(mappedName);
                    if (byMapped.isPresent()) {
                        return byMapped.get();
                    }
                }
            }
        }

        return null;
    }

    private String mapMockCodeToName(String code) {
        return switch (code.toLowerCase()) {
            case "f1" -> "Classic Veg Burger";
            case "f2" -> "Smoky Chicken Burger";
            case "f3" -> "Peri Peri Fries";
            case "f4" -> "Paneer Tikka Pizza";
            case "f5" -> "Margherita Classica";
            case "f6" -> "Chicken Pepperoni";
            case "f7" -> "Hyderabadi Chicken Biryani";
            case "f8" -> "Veg Dum Biryani";
            case "f9" -> "Chicken 65";
            case "f10" -> "Butter Naan";
            case "f11" -> "Gujarati Mini Thali";
            case "f12" -> "Paneer Butter Masala";
            case "f13" -> "Khaman Dhokla";
            case "f14" -> "Masala Chaas";
            case "f15" -> "Masala Dosa";
            case "f16" -> "Idli Sambar";
            case "f17" -> "Filter Coffee";
            case "f18" -> "Quinoa Power Bowl";
            case "f19" -> "Grilled Chicken Salad";
            case "f20" -> "Berry Smoothie";
            case "f21" -> "Veg Hakka Noodles";
            case "f22" -> "Chilli Chicken";
            case "f23" -> "Chocolate Brownie";
            case "f24" -> "Mango Cheesecake";
            default -> null;
        };
    }

    private User getCustomer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer account not found"));

        if (user.getRole() != Role.CUSTOMER) {
            throw new AccessDeniedException("Only customers are authorized to use the shopping cart");
        }

        return user;
    }

    private void validateCartOwnership(Cart cart, User customer) {
        if (!cart.getUser().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Cart item not found");
        }
    }
}

