package com.cravely.backend.dto;

import com.cravely.backend.entity.CartItem;
import com.cravely.backend.entity.FoodType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.math.RoundingMode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemResponse {

    private Long cartItemId;
    private Long foodItemId;
    private String foodName;
    private String imageUrl;
    private FoodType foodType;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal itemTotal;

    public CartItemResponse() {
    }

    public static CartItemResponse fromEntity(CartItem cartItem) {
        if (cartItem == null || cartItem.getFoodItem() == null) {
            return null;
        }

        CartItemResponse response = new CartItemResponse();
        response.setCartItemId(cartItem.getId());
        response.setFoodItemId(cartItem.getFoodItem().getId());
        response.setFoodName(cartItem.getFoodItem().getName());
        response.setImageUrl(cartItem.getFoodItem().getImageUrl());
        response.setFoodType(cartItem.getFoodItem().getFoodType());
        response.setQuantity(cartItem.getQuantity());

        // Always use the latest database unit price
        BigDecimal unitPrice = cartItem.getFoodItem().getPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())).setScale(2, RoundingMode.HALF_UP);

        response.setUnitPrice(unitPrice);
        response.setItemTotal(itemTotal);
        return response;
    }

    public Long getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(Long cartItemId) {
        this.cartItemId = cartItemId;
    }

    public Long getFoodItemId() {
        return foodItemId;
    }

    public void setFoodItemId(Long foodItemId) {
        this.foodItemId = foodItemId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public FoodType getFoodType() {
        return foodType;
    }

    public void setFoodType(FoodType foodType) {
        this.foodType = foodType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getItemTotal() {
        return itemTotal;
    }

    public void setItemTotal(BigDecimal itemTotal) {
        this.itemTotal = itemTotal;
    }
}

