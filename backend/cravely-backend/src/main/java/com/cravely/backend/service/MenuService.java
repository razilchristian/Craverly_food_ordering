package com.cravely.backend.service;

import com.cravely.backend.dto.FoodItemResponse;
import com.cravely.backend.dto.MenuResponse;
import com.cravely.backend.entity.FoodCategory;
import com.cravely.backend.entity.FoodItem;
import com.cravely.backend.entity.Restaurant;
import com.cravely.backend.entity.RestaurantStatus;
import com.cravely.backend.exception.ResourceNotFoundException;
import com.cravely.backend.repository.FoodCategoryRepository;
import com.cravely.backend.repository.FoodItemRepository;
import com.cravely.backend.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final RestaurantRepository restaurantRepository;
    private final FoodCategoryRepository categoryRepository;
    private final FoodItemRepository foodItemRepository;

    public MenuService(RestaurantRepository restaurantRepository,
                       FoodCategoryRepository categoryRepository,
                       FoodItemRepository foodItemRepository) {
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.foodItemRepository = foodItemRepository;
    }

    @Transactional(readOnly = true)
    public MenuResponse getRestaurantMenu(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findByIdAndStatusAndActiveTrue(restaurantId, RestaurantStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found or is currently not available"));

        List<FoodCategory> activeCategories = categoryRepository.findByRestaurantIdAndActiveTrue(restaurantId);
        List<FoodItem> availableItems = foodItemRepository.findByRestaurantIdAndAvailableTrue(restaurantId);

        // Group available items by category ID
        Map<Long, List<FoodItem>> itemsByCategoryId = availableItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.getCategory().getActive()))
                .collect(Collectors.groupingBy(item -> item.getCategory().getId()));

        List<MenuResponse.CategoryMenuSection> sections = new ArrayList<>();
        for (FoodCategory category : activeCategories) {
            List<FoodItem> categoryItems = itemsByCategoryId.getOrDefault(category.getId(), List.of());
            List<FoodItemResponse> itemResponses = categoryItems.stream()
                    .map(FoodItemResponse::fromEntity)
                    .collect(Collectors.toList());

            sections.add(new MenuResponse.CategoryMenuSection(
                    category.getId(),
                    category.getName(),
                    category.getDescription(),
                    itemResponses
            ));
        }

        return new MenuResponse(restaurant.getId(), restaurant.getName(), sections);
    }
}

