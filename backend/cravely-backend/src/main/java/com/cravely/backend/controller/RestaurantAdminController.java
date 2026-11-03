package com.cravely.backend.controller;

import com.cravely.backend.dto.ApiResponse;
import com.cravely.backend.dto.RestaurantResponse;
import com.cravely.backend.service.RestaurantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/restaurants")
@PreAuthorize("hasRole('ADMIN')")
public class RestaurantAdminController {

    private final RestaurantService restaurantService;

    public RestaurantAdminController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getAllRestaurants() {
        List<RestaurantResponse> restaurants = restaurantService.getAllRestaurants();
        return ResponseEntity.ok(ApiResponse.success("All restaurants retrieved successfully", restaurants));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getPendingRestaurants() {
        List<RestaurantResponse> pending = restaurantService.getPendingRestaurants();
        return ResponseEntity.ok(ApiResponse.success("Pending restaurants retrieved successfully", pending));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getRestaurantById(@PathVariable Long id) {
        RestaurantResponse restaurant = restaurantService.getRestaurantByIdAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant retrieved successfully", restaurant));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<RestaurantResponse>> approveRestaurant(@PathVariable Long id) {
        RestaurantResponse approved = restaurantService.approveRestaurant(id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant approved successfully", approved));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<RestaurantResponse>> rejectRestaurant(@PathVariable Long id) {
        RestaurantResponse rejected = restaurantService.rejectRestaurant(id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant rejected successfully", rejected));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<RestaurantResponse>> activateRestaurant(@PathVariable Long id) {
        RestaurantResponse activated = restaurantService.activateRestaurantByAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant activated successfully", activated));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<RestaurantResponse>> deactivateRestaurant(@PathVariable Long id) {
        RestaurantResponse deactivated = restaurantService.deactivateRestaurantByAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant deactivated successfully", deactivated));
    }
}

