package com.cravely.backend.controller;

import com.cravely.backend.dto.ApiResponse;
import com.cravely.backend.dto.FoodCategoryResponse;
import com.cravely.backend.dto.FoodItemResponse;
import com.cravely.backend.service.FoodCategoryService;
import com.cravely.backend.service.FoodItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class MenuAdminController {

    private final FoodCategoryService categoryService;
    private final FoodItemService foodItemService;

    public MenuAdminController(FoodCategoryService categoryService, FoodItemService foodItemService) {
        this.categoryService = categoryService;
        this.foodItemService = foodItemService;
    }

    @GetMapping("/restaurants/{restaurantId}/categories")
    public ResponseEntity<ApiResponse<List<FoodCategoryResponse>>> getAdminCategories(@PathVariable Long restaurantId) {
        List<FoodCategoryResponse> categories = categoryService.getAdminCategories(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Admin categories retrieved successfully", categories));
    }

    @GetMapping("/restaurants/{restaurantId}/items")
    public ResponseEntity<ApiResponse<List<FoodItemResponse>>> getAdminRestaurantItems(@PathVariable Long restaurantId) {
        List<FoodItemResponse> items = foodItemService.getAdminItems(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Admin restaurant items retrieved successfully", items));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<FoodItemResponse>>> getAllAdminItems() {
        List<FoodItemResponse> items = foodItemService.getAllAdminItems();
        return ResponseEntity.ok(ApiResponse.success("All food items retrieved successfully", items));
    }

    @PatchMapping("/items/{itemId}/deactivate")
    public ResponseEntity<ApiResponse<FoodItemResponse>> deactivateItem(@PathVariable Long itemId) {
        FoodItemResponse updated = foodItemService.setAdminItemStatus(itemId, false);
        return ResponseEntity.ok(ApiResponse.success("Food item deactivated successfully", updated));
    }

    @PatchMapping("/items/{itemId}/activate")
    public ResponseEntity<ApiResponse<FoodItemResponse>> activateItem(@PathVariable Long itemId) {
        FoodItemResponse updated = foodItemService.setAdminItemStatus(itemId, true);
        return ResponseEntity.ok(ApiResponse.success("Food item activated successfully", updated));
    }
}

