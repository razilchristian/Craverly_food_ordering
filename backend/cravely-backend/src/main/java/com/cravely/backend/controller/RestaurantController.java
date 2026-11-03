package com.cravely.backend.controller;

import com.cravely.backend.dto.ApiResponse;
import com.cravely.backend.dto.RestaurantResponse;
import com.cravely.backend.service.RestaurantService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> getApprovedRestaurants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "id"));
        Page<RestaurantResponse> restaurants = restaurantService.getApprovedActiveRestaurants(pageable);
        return ResponseEntity.ok(ApiResponse.success("Approved restaurants retrieved successfully", restaurants));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getRestaurantById(@PathVariable Long id) {
        RestaurantResponse restaurant = restaurantService.getApprovedRestaurantById(id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant details retrieved successfully", restaurant));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> searchRestaurants(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "id"));
        Page<RestaurantResponse> restaurants = restaurantService.searchRestaurantsByName(name, pageable);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", restaurants));
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> getRestaurantsByCity(
            @PathVariable String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "id"));
        Page<RestaurantResponse> restaurants = restaurantService.filterRestaurantsByCity(city, pageable);
        return ResponseEntity.ok(ApiResponse.success("Restaurants in " + city + " retrieved successfully", restaurants));
    }

    @GetMapping("/cuisine/{cuisine}")
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> getRestaurantsByCuisine(
            @PathVariable String cuisine,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "id"));
        Page<RestaurantResponse> restaurants = restaurantService.filterRestaurantsByCuisine(cuisine, pageable);
        return ResponseEntity.ok(ApiResponse.success("Restaurants with cuisine " + cuisine + " retrieved successfully", restaurants));
    }
}

