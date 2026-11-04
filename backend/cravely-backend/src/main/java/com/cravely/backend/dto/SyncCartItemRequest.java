package com.cravely.backend.dto;

public class SyncCartItemRequest {

    private Long foodItemId;
    private String itemCode;
    private String name;
    private String restaurantId;
    private Integer quantity;

    public SyncCartItemRequest() {
    }

    public SyncCartItemRequest(Long foodItemId, String itemCode, String name, String restaurantId, Integer quantity) {
        this.foodItemId = foodItemId;
        this.itemCode = itemCode;
        this.name = name;
        this.restaurantId = restaurantId;
        this.quantity = quantity;
    }

    public Long getFoodItemId() {
        return foodItemId;
    }

    public void setFoodItemId(Long foodItemId) {
        this.foodItemId = foodItemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}

