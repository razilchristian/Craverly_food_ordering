package com.cravely.backend.controller;

import com.cravely.backend.dto.ApiResponse;
import com.cravely.backend.dto.FoodCategoryResponse;
import com.cravely.backend.dto.FoodItemResponse;
import com.cravely.backend.dto.MenuResponse;
import com.cravely.backend.entity.FoodType;
import com.cravely.backend.service.FoodCategoryService;
import com.cravely.backend.service.FoodItemService;
import com.cravely.backend.service.MenuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MenuController {

    private final MenuService menuService;
    private final FoodCategoryService categoryService;
    private final FoodItemService foodItemService;

    public MenuController(MenuService menuService,
                          FoodCategoryService categoryService,
                          FoodItemService foodItemService) {
        this.menuService = menuService;
        this.categoryService = categoryService;
        this.foodItemService = foodItemService;
    }

    @GetMapping("/api/restaurants/{restaurantId}/menu")
    public ResponseEntity<ApiResponse<MenuResponse>> getRestaurantMenu(@PathVariable Long restaurantId) {
        MenuResponse menu = menuService.getRestaurantMenu(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Menu retrieved successfully", menu));
    }

    @GetMapping("/api/restaurants/{restaurantId}/categories")
    public ResponseEntity<ApiResponse<List<FoodCategoryResponse>>> getActiveCategories(@PathVariable Long restaurantId) {
        List<FoodCategoryResponse> categories = categoryService.getPublicActiveCategories(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }

    @GetMapping("/api/restaurants/{restaurantId}/items")
    public ResponseEntity<ApiResponse<List<FoodItemResponse>>> getAvailableItems(@PathVariable Long restaurantId) {
        List<FoodItemResponse> items = foodItemService.getPublicAvailableItems(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Food items retrieved successfully", items));
    }

    @GetMapping("/api/items/{itemId}")
    public ResponseEntity<ApiResponse<FoodItemResponse>> getItemDetails(@PathVariable Long itemId) {
        FoodItemResponse item = foodItemService.getPublicItemById(itemId);
        return ResponseEntity.ok(ApiResponse.success("Food item details retrieved successfully", item));
    }

    @GetMapping("/api/restaurants/{restaurantId}/items/search")
    public ResponseEntity<ApiResponse<List<FoodItemResponse>>> searchItems(
            @PathVariable Long restaurantId,
            @RequestParam String name) {
        List<FoodItemResponse> items = foodItemService.searchPublicItemsByName(restaurantId, name);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", items));
    }

    @GetMapping("/api/restaurants/{restaurantId}/items/type/{foodType}")
    public ResponseEntity<ApiResponse<List<FoodItemResponse>>> filterItemsByType(
            @PathVariable Long restaurantId,
            @PathVariable FoodType foodType) {
        List<FoodItemResponse> items = foodItemService.filterPublicItemsByType(restaurantId, foodType);
        return ResponseEntity.ok(ApiResponse.success("Filtered items retrieved successfully", items));
    }

    @GetMapping("/api/restaurants/{restaurantId}/categories/{categoryId}/items")
    public ResponseEntity<ApiResponse<List<FoodItemResponse>>> getItemsByCategory(
            @PathVariable Long restaurantId,
            @PathVariable Long categoryId) {
        List<FoodItemResponse> items = foodItemService.getPublicItemsByCategory(restaurantId, categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category items retrieved successfully", items));
    }
}

