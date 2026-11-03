package com.cravely.backend.controller;

import com.cravely.backend.dto.ApiResponse;
import com.cravely.backend.dto.FoodItemCreateRequest;
import com.cravely.backend.dto.FoodItemResponse;
import com.cravely.backend.dto.FoodItemUpdateRequest;
import com.cravely.backend.service.FoodItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class FoodItemController {

    private final FoodItemService foodItemService;

    public FoodItemController(FoodItemService foodItemService) {
        this.foodItemService = foodItemService;
    }

    @PostMapping("/restaurants/{restaurantId}/items")
    public ResponseEntity<ApiResponse<FoodItemResponse>> createFoodItem(
            @PathVariable Long restaurantId,
            @Valid @RequestBody FoodItemCreateRequest request,
            Principal principal) {
        FoodItemResponse created = foodItemService.createFoodItem(principal.getName(), restaurantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Food item created successfully", created));
    }

    @GetMapping("/restaurants/{restaurantId}/items")
    public ResponseEntity<ApiResponse<List<FoodItemResponse>>> getOwnerFoodItems(
            @PathVariable Long restaurantId,
            Principal principal) {
        List<FoodItemResponse> items = foodItemService.getOwnerFoodItems(principal.getName(), restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Food items retrieved successfully", items));
    }

    @GetMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<FoodItemResponse>> getOwnerFoodItemById(
            @PathVariable Long itemId,
            Principal principal) {
        FoodItemResponse item = foodItemService.getOwnerFoodItemById(principal.getName(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Food item details retrieved successfully", item));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<FoodItemResponse>> updateFoodItem(
            @PathVariable Long itemId,
            @Valid @RequestBody FoodItemUpdateRequest request,
            Principal principal) {
        FoodItemResponse updated = foodItemService.updateFoodItem(principal.getName(), itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Food item updated successfully", updated));
    }

    @PatchMapping("/items/{itemId}/availability")
    public ResponseEntity<ApiResponse<FoodItemResponse>> changeFoodAvailability(
            @PathVariable Long itemId,
            @RequestParam(required = false) Boolean available,
            Principal principal) {
        FoodItemResponse updated = foodItemService.setFoodItemAvailability(principal.getName(), itemId, available);
        return ResponseEntity.ok(ApiResponse.success("Food item availability updated successfully", updated));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteFoodItem(
            @PathVariable Long itemId,
            Principal principal) {
        foodItemService.deleteFoodItem(principal.getName(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Food item deleted successfully"));
    }
}

