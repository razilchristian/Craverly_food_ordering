package com.cravely.backend.controller;

import com.cravely.backend.dto.ApiResponse;
import com.cravely.backend.dto.RestaurantCreateRequest;
import com.cravely.backend.dto.RestaurantResponse;
import com.cravely.backend.dto.RestaurantUpdateRequest;
import com.cravely.backend.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/owner/restaurants")
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class RestaurantOwnerController {

    private final RestaurantService restaurantService;

    public RestaurantOwnerController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RestaurantResponse>> createRestaurant(
            Principal principal,
            @Valid @RequestBody RestaurantCreateRequest request) {
        RestaurantResponse created = restaurantService.createRestaurant(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Restaurant submitted for approval successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getOwnerRestaurants(Principal principal) {
        List<RestaurantResponse> restaurants = restaurantService.getOwnerRestaurants(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Owner restaurants retrieved successfully", restaurants));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getOwnerRestaurantById(
            @PathVariable Long id,
            Principal principal) {
        RestaurantResponse restaurant = restaurantService.getOwnerRestaurantById(principal.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant details retrieved successfully", restaurant));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> updateRestaurant(
            @PathVariable Long id,
            Principal principal,
            @Valid @RequestBody RestaurantUpdateRequest request) {
        RestaurantResponse updated = restaurantService.updateRestaurant(principal.getName(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Restaurant updated successfully", updated));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<RestaurantResponse>> deactivateRestaurant(
            @PathVariable Long id,
            Principal principal) {
        RestaurantResponse deactivated = restaurantService.deactivateRestaurantByOwner(principal.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant deactivated successfully", deactivated));
    }
}
