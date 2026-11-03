package com.cravely.backend.controller;

import com.cravely.backend.dto.AddToCartRequest;
import com.cravely.backend.dto.ApiResponse;
import com.cravely.backend.dto.CartResponse;
import com.cravely.backend.dto.UpdateCartItemRequest;
import com.cravely.backend.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Principal principal) {
        CartResponse cart = cartService.getCart(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Cart fetched successfully", cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Principal principal) {
        CartResponse cart = cartService.addToCart(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart successfully", cart));
    }

    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItemQuantity(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Principal principal) {
        CartResponse cart = cartService.updateCartItemQuantity(principal.getName(), cartItemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item quantity updated successfully", cart));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @PathVariable Long cartItemId,
            Principal principal) {
        CartResponse cart = cartService.removeCartItem(principal.getName(), cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart successfully", cart));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(Principal principal) {
        CartResponse cart = cartService.clearCart(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully", cart));
    }
}

