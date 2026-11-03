package com.cravely.backend.dto;

import com.cravely.backend.entity.Cart;
import com.cravely.backend.entity.CartItem;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartResponse {

    private Long cartId;
    private RestaurantSummary restaurant;
    private List<CartItemResponse> items = new ArrayList<>();
    private Integer totalItems = 0;
    private BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public CartResponse() {
    }

    public static CartResponse empty() {
        CartResponse emptyResponse = new CartResponse();
        emptyResponse.setCartId(null);
        emptyResponse.setRestaurant(null);
        emptyResponse.setItems(new ArrayList<>());
        emptyResponse.setTotalItems(0);
        emptyResponse.setSubtotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        return emptyResponse;
    }

    public static CartResponse fromEntity(Cart cart) {
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            CartResponse res = empty();
            if (cart != null) {
                res.setCartId(cart.getId());
            }
            return res;
        }

        CartResponse response = new CartResponse();
        response.setCartId(cart.getId());

        if (cart.getRestaurant() != null) {
            response.setRestaurant(new RestaurantSummary(
                    cart.getRestaurant().getId(),
                    cart.getRestaurant().getName()
            ));
        }

        List<CartItemResponse> itemResponses = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            CartItemResponse itemResp = CartItemResponse.fromEntity(item);
            if (itemResp != null) {
                itemResponses.add(itemResp);
                totalQuantity += itemResp.getQuantity();
                calculatedSubtotal = calculatedSubtotal.add(itemResp.getItemTotal());
            }
        }

        response.setItems(itemResponses);
        response.setTotalItems(totalQuantity);
        response.setSubtotal(calculatedSubtotal.setScale(2, RoundingMode.HALF_UP));

        return response;
    }

    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }

    public RestaurantSummary getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(RestaurantSummary restaurant) {
        this.restaurant = restaurant;
    }

    public List<CartItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponse> items) {
        this.items = items;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public static class RestaurantSummary {
        private Long id;
        private String name;

        public RestaurantSummary() {
        }

        public RestaurantSummary(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

