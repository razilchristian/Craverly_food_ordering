package com.cravely.backend.controller;

import com.cravely.backend.dto.ApiResponse;
import com.cravely.backend.dto.FoodCategoryCreateRequest;
import com.cravely.backend.dto.FoodCategoryResponse;
import com.cravely.backend.dto.FoodCategoryUpdateRequest;
import com.cravely.backend.service.FoodCategoryService;
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
public class FoodCategoryController {

    private final FoodCategoryService categoryService;

    public FoodCategoryController(FoodCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping("/restaurants/{restaurantId}/categories")
    public ResponseEntity<ApiResponse<FoodCategoryResponse>> createCategory(
            @PathVariable Long restaurantId,
            @Valid @RequestBody FoodCategoryCreateRequest request,
            Principal principal) {
        FoodCategoryResponse created = categoryService.createCategory(principal.getName(), restaurantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", created));
    }

    @GetMapping("/restaurants/{restaurantId}/categories")
    public ResponseEntity<ApiResponse<List<FoodCategoryResponse>>> getOwnerCategories(
            @PathVariable Long restaurantId,
            Principal principal) {
        List<FoodCategoryResponse> categories = categoryService.getOwnerCategories(principal.getName(), restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }

    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<FoodCategoryResponse>> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody FoodCategoryUpdateRequest request,
            Principal principal) {
        FoodCategoryResponse updated = categoryService.updateCategory(principal.getName(), categoryId, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", updated));
    }

    @PatchMapping("/categories/{categoryId}/deactivate")
    public ResponseEntity<ApiResponse<FoodCategoryResponse>> deactivateCategory(
            @PathVariable Long categoryId,
            Principal principal) {
        FoodCategoryResponse deactivated = categoryService.deactivateCategory(principal.getName(), categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category deactivated successfully", deactivated));
    }
}

