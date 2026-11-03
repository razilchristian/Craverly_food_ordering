package com.cravely.backend.service;

import com.cravely.backend.dto.FoodItemCreateRequest;
import com.cravely.backend.dto.FoodItemResponse;
import com.cravely.backend.dto.FoodItemUpdateRequest;
import com.cravely.backend.entity.FoodCategory;
import com.cravely.backend.entity.FoodItem;
import com.cravely.backend.entity.FoodType;
import com.cravely.backend.entity.Restaurant;
import com.cravely.backend.entity.RestaurantStatus;
import com.cravely.backend.exception.ResourceNotFoundException;
import com.cravely.backend.repository.FoodCategoryRepository;
import com.cravely.backend.repository.FoodItemRepository;
import com.cravely.backend.repository.RestaurantRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FoodItemService {

    private final FoodItemRepository foodItemRepository;
    private final FoodCategoryRepository categoryRepository;
    private final RestaurantRepository restaurantRepository;

    public FoodItemService(FoodItemRepository foodItemRepository,
                           FoodCategoryRepository categoryRepository,
                           RestaurantRepository restaurantRepository) {
        this.foodItemRepository = foodItemRepository;
        this.categoryRepository = categoryRepository;
        this.restaurantRepository = restaurantRepository;
    }

    // ==========================================
    // OWNER FUNCTIONALITY
    // ==========================================

    @Transactional
    public FoodItemResponse createFoodItem(String ownerEmail, Long restaurantId, FoodItemCreateRequest request) {
        Restaurant restaurant = getAndValidateRestaurantOwnership(restaurantId, ownerEmail);

        if (restaurant.getStatus() != RestaurantStatus.APPROVED || !Boolean.TRUE.equals(restaurant.getActive())) {
            throw new IllegalStateException("Cannot create food items for an unapproved or inactive restaurant");
        }

        FoodCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        if (!category.getRestaurant().getId().equals(restaurantId)) {
            throw new IllegalArgumentException("Selected category does not belong to this restaurant");
        }

        if (Boolean.FALSE.equals(category.getActive())) {
            throw new IllegalStateException("Cannot add food items to a deactivated category");
        }

        FoodItem item = new FoodItem(
                request.getName().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                restaurant,
                category,
                request.getPrice(),
                request.getImageUrl() != null ? request.getImageUrl().trim() : null,
                request.getFoodType(),
                request.getAvailable() != null ? request.getAvailable() : true,
                request.getPreparationTime()
        );

        FoodItem saved = foodItemRepository.save(item);
        return FoodItemResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<FoodItemResponse> getOwnerFoodItems(String ownerEmail, Long restaurantId) {
        getAndValidateRestaurantOwnership(restaurantId, ownerEmail);
        return foodItemRepository.findByRestaurantId(restaurantId).stream()
                .map(FoodItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FoodItemResponse getOwnerFoodItemById(String ownerEmail, Long itemId) {
        FoodItem item = foodItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + itemId));

        validateRestaurantOwnership(item.getRestaurant(), ownerEmail);
        return FoodItemResponse.fromEntity(item);
    }

    @Transactional
    public FoodItemResponse updateFoodItem(String ownerEmail, Long itemId, FoodItemUpdateRequest request) {
        FoodItem item = foodItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + itemId));

        validateRestaurantOwnership(item.getRestaurant(), ownerEmail);

        FoodCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        if (!category.getRestaurant().getId().equals(item.getRestaurant().getId())) {
            throw new IllegalArgumentException("Selected category does not belong to this restaurant");
        }

        item.setName(request.getName().trim());
        item.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        item.setCategory(category);
        item.setPrice(request.getPrice());
        item.setImageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null);
        item.setFoodType(request.getFoodType());
        if (request.getAvailable() != null) {
            item.setAvailable(request.getAvailable());
        }
        item.setPreparationTime(request.getPreparationTime());

        FoodItem updated = foodItemRepository.save(item);
        return FoodItemResponse.fromEntity(updated);
    }

    @Transactional
    public FoodItemResponse setFoodItemAvailability(String ownerEmail, Long itemId, Boolean available) {
        FoodItem item = foodItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + itemId));

        validateRestaurantOwnership(item.getRestaurant(), ownerEmail);

        if (available != null) {
            item.setAvailable(available);
        } else {
            item.setAvailable(!Boolean.TRUE.equals(item.getAvailable()));
        }

        FoodItem updated = foodItemRepository.save(item);
        return FoodItemResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteFoodItem(String ownerEmail, Long itemId) {
        FoodItem item = foodItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + itemId));

        validateRestaurantOwnership(item.getRestaurant(), ownerEmail);
        foodItemRepository.delete(item);
    }

    // ==========================================
    // CUSTOMER / PUBLIC FUNCTIONALITY
    // ==========================================

    @Transactional(readOnly = true)
    public List<FoodItemResponse> getPublicAvailableItems(Long restaurantId) {
        validatePublicRestaurant(restaurantId);

        return foodItemRepository.findByRestaurantIdAndAvailableTrue(restaurantId).stream()
                .filter(item -> Boolean.TRUE.equals(item.getCategory().getActive()))
                .map(FoodItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FoodItemResponse getPublicItemById(Long itemId) {
        FoodItem item = foodItemRepository.findByIdAndAvailableTrue(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found or is currently unavailable"));

        Restaurant restaurant = item.getRestaurant();
        if (restaurant.getStatus() != RestaurantStatus.APPROVED || !Boolean.TRUE.equals(restaurant.getActive())
                || !Boolean.TRUE.equals(item.getCategory().getActive())) {
            throw new ResourceNotFoundException("Food item is currently not available");
        }

        return FoodItemResponse.fromEntity(item);
    }

    @Transactional(readOnly = true)
    public List<FoodItemResponse> searchPublicItemsByName(Long restaurantId, String name) {
        validatePublicRestaurant(restaurantId);

        return foodItemRepository.findByRestaurantIdAndNameContainingIgnoreCaseAndAvailableTrue(restaurantId, name).stream()
                .filter(item -> Boolean.TRUE.equals(item.getCategory().getActive()))
                .map(FoodItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FoodItemResponse> filterPublicItemsByType(Long restaurantId, FoodType foodType) {
        validatePublicRestaurant(restaurantId);

        return foodItemRepository.findByRestaurantIdAndFoodTypeAndAvailableTrue(restaurantId, foodType).stream()
                .filter(item -> Boolean.TRUE.equals(item.getCategory().getActive()))
                .map(FoodItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FoodItemResponse> getPublicItemsByCategory(Long restaurantId, Long categoryId) {
        validatePublicRestaurant(restaurantId);

        FoodCategory category = categoryRepository.findByIdAndRestaurantId(categoryId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found in this restaurant"));

        if (!Boolean.TRUE.equals(category.getActive())) {
            return List.of();
        }

        return foodItemRepository.findByCategoryIdAndAvailableTrue(categoryId).stream()
                .map(FoodItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ==========================================
    // ADMIN FUNCTIONALITY
    // ==========================================

    @Transactional(readOnly = true)
    public List<FoodItemResponse> getAdminItems(Long restaurantId) {
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));

        return foodItemRepository.findByRestaurantId(restaurantId).stream()
                .map(FoodItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FoodItemResponse> getAllAdminItems() {
        return foodItemRepository.findAll().stream()
                .map(FoodItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public FoodItemResponse setAdminItemStatus(Long itemId, boolean available) {
        FoodItem item = foodItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + itemId));

        item.setAvailable(available);
        FoodItem updated = foodItemRepository.save(item);
        return FoodItemResponse.fromEntity(updated);
    }

    // ==========================================
    // HELPERS
    // ==========================================

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

    private Restaurant validatePublicRestaurant(Long restaurantId) {
        return restaurantRepository.findByIdAndStatusAndActiveTrue(restaurantId, RestaurantStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found or is currently not available"));
    }
}

