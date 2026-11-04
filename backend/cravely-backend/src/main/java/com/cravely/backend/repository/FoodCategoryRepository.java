package com.cravely.backend.repository;

import com.cravely.backend.entity.FoodCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {

    List<FoodCategory> findByRestaurantId(Long restaurantId);

    List<FoodCategory> findByRestaurantIdAndActiveTrue(Long restaurantId);

    Optional<FoodCategory> findByIdAndRestaurantId(Long id, Long restaurantId);

    Optional<FoodCategory> findFirstByRestaurantIdAndNameIgnoreCase(Long restaurantId, String name);

    boolean existsByRestaurantIdAndNameIgnoreCase(Long restaurantId, String name);

    boolean existsByRestaurantIdAndNameIgnoreCaseAndIdNot(Long restaurantId, String name, Long id);
}

