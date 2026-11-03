package com.cravely.backend.service;

import com.cravely.backend.dto.FoodCategoryCreateRequest;
import com.cravely.backend.dto.FoodCategoryResponse;
import com.cravely.backend.dto.FoodCategoryUpdateRequest;
import com.cravely.backend.entity.FoodCategory;
import com.cravely.backend.entity.Restaurant;
import com.cravely.backend.entity.RestaurantStatus;
import com.cravely.backend.exception.ResourceNotFoundException;
import com.cravely.backend.repository.FoodCategoryRepository;
import com.cravely.backend.repository.RestaurantRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FoodCategoryService {

    private final FoodCategoryRepository categoryRepository;
    private final RestaurantRepository restaurantRepository;

    public FoodCategoryService(FoodCategoryRepository categoryRepository, RestaurantRepository restaurantRepository) {
        this.categoryRepository = categoryRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Transactional
    public FoodCategoryResponse createCategory(String ownerEmail, Long restaurantId, FoodCategoryCreateRequest request) {
        Restaurant restaurant = getAndValidateRestaurantOwnership(restaurantId, ownerEmail);

        if (restaurant.getStatus() != RestaurantStatus.APPROVED || !Boolean.TRUE.equals(restaurant.getActive())) {
            throw new IllegalStateException("Cannot create categories for an unapproved or inactive restaurant");
        }

        String categoryName = request.getName().trim();
        if (categoryRepository.existsByRestaurantIdAndNameIgnoreCase(restaurantId, categoryName)) {
            throw new IllegalArgumentException("A category with the name '" + categoryName + "' already exists in this restaurant");
        }

        FoodCategory category = new FoodCategory(
                categoryName,
                request.getDescription() != null ? request.getDescription().trim() : null,
                restaurant
        );

        FoodCategory saved = categoryRepository.save(category);
        return FoodCategoryResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<FoodCategoryResponse> getOwnerCategories(String ownerEmail, Long restaurantId) {
        getAndValidateRestaurantOwnership(restaurantId, ownerEmail);
        return categoryRepository.findByRestaurantId(restaurantId).stream()
                .map(FoodCategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public FoodCategoryResponse updateCategory(String ownerEmail, Long categoryId, FoodCategoryUpdateRequest request) {
        FoodCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Food category not found with id: " + categoryId));

        validateRestaurantOwnership(category.getRestaurant(), ownerEmail);

        String newName = request.getName().trim();
        if (categoryRepository.existsByRestaurantIdAndNameIgnoreCaseAndIdNot(category.getRestaurant().getId(), newName, categoryId)) {
            throw new IllegalArgumentException("Another category with the name '" + newName + "' already exists in this restaurant");
        }

        category.setName(newName);
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        FoodCategory updated = categoryRepository.save(category);
        return FoodCategoryResponse.fromEntity(updated);
    }

    @Transactional
    public FoodCategoryResponse deactivateCategory(String ownerEmail, Long categoryId) {
        FoodCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Food category not found with id: " + categoryId));

        validateRestaurantOwnership(category.getRestaurant(), ownerEmail);

        category.setActive(false);
        FoodCategory saved = categoryRepository.save(category);
        return FoodCategoryResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<FoodCategoryResponse> getPublicActiveCategories(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findByIdAndStatusAndActiveTrue(restaurantId, RestaurantStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found or is currently not available"));

        return categoryRepository.findByRestaurantIdAndActiveTrue(restaurant.getId()).stream()
                .map(FoodCategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FoodCategoryResponse> getAdminCategories(Long restaurantId) {
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));

        return categoryRepository.findByRestaurantId(restaurantId).stream()
                .map(FoodCategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private Restaurant getAndValidateRestaurantOwnership(Long restaurantId, String ownerEmail) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));
        validateRestaurantOwnership(restaurant, ownerEmail);
        return restaurant;
    }

    private void validateRestaurantOwnership(Restaurant restaurant, String ownerEmail) {
        if (!restaurant.getOwner().getEmail().equalsIgnoreCase(ownerEmail)) {
            throw new AccessDeniedException("Unauthorized: You do not own this restaurant");
        }
    }
}

