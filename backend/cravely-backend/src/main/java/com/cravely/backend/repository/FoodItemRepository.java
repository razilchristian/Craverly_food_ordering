package com.cravely.backend.repository;

import com.cravely.backend.entity.FoodItem;
import com.cravely.backend.entity.FoodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    List<FoodItem> findByRestaurantId(Long restaurantId);

    List<FoodItem> findByRestaurantIdAndAvailableTrue(Long restaurantId);

    List<FoodItem> findByCategoryId(Long categoryId);

    List<FoodItem> findByCategoryIdAndAvailableTrue(Long categoryId);

    List<FoodItem> findByRestaurantIdAndFoodTypeAndAvailableTrue(Long restaurantId, FoodType foodType);

    List<FoodItem> findByRestaurantIdAndNameContainingIgnoreCaseAndAvailableTrue(Long restaurantId, String name);

    Optional<FoodItem> findByIdAndAvailableTrue(Long id);

    Optional<FoodItem> findByIdAndRestaurantId(Long id, Long restaurantId);
}

