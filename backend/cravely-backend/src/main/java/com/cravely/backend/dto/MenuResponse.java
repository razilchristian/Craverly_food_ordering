package com.cravely.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuResponse {

    private Long restaurantId;
    private String restaurantName;
    private List<CategoryMenuSection> categories = new ArrayList<>();

    public MenuResponse() {
    }

    public MenuResponse(Long restaurantId, String restaurantName, List<CategoryMenuSection> categories) {
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.categories = categories;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public List<CategoryMenuSection> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryMenuSection> categories) {
        this.categories = categories;
    }

    public static class CategoryMenuSection {
        private Long categoryId;
        private String categoryName;
        private String description;
        private List<FoodItemResponse> items = new ArrayList<>();

        public CategoryMenuSection() {
        }

        public CategoryMenuSection(Long categoryId, String categoryName, String description, List<FoodItemResponse> items) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.description = description;
            this.items = items;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<FoodItemResponse> getItems() {
            return items;
        }

        public void setItems(List<FoodItemResponse> items) {
            this.items = items;
        }
    }
}

