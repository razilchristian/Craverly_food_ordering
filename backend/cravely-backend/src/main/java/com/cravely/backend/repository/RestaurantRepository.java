package com.cravely.backend.repository;

import com.cravely.backend.entity.Restaurant;
import com.cravely.backend.entity.RestaurantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findByOwnerId(Long ownerId);

    Optional<Restaurant> findByIdAndOwnerId(Long id, Long ownerId);

    List<Restaurant> findByStatus(RestaurantStatus status);

    Page<Restaurant> findByStatusAndActiveTrue(RestaurantStatus status, Pageable pageable);

    Page<Restaurant> findByStatusAndActiveTrueAndNameContainingIgnoreCase(RestaurantStatus status, String name, Pageable pageable);

    Page<Restaurant> findByStatusAndActiveTrueAndCityIgnoreCase(RestaurantStatus status, String city, Pageable pageable);

    Page<Restaurant> findByStatusAndActiveTrueAndCuisineTypeIgnoreCase(RestaurantStatus status, String cuisineType, Pageable pageable);

    Optional<Restaurant> findByIdAndStatusAndActiveTrue(Long id, RestaurantStatus status);
}

