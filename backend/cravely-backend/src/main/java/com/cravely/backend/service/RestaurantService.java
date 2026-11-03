package com.cravely.backend.service;

import com.cravely.backend.dto.RestaurantCreateRequest;
import com.cravely.backend.dto.RestaurantResponse;
import com.cravely.backend.dto.RestaurantUpdateRequest;
import com.cravely.backend.entity.Restaurant;
import com.cravely.backend.entity.RestaurantStatus;
import com.cravely.backend.entity.Role;
import com.cravely.backend.entity.User;
import com.cravely.backend.exception.ResourceNotFoundException;
import com.cravely.backend.repository.RestaurantRepository;
import com.cravely.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    public RestaurantService(RestaurantRepository restaurantRepository, UserRepository userRepository) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
    }

    // ==========================================
    // OWNER FUNCTIONALITY
    // ==========================================

    @Transactional
    public RestaurantResponse createRestaurant(String ownerEmail, RestaurantCreateRequest request) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Owner account not found"));

        if (owner.getRole() != Role.RESTAURANT_OWNER) {
            throw new AccessDeniedException("Only registered restaurant owners can create restaurants");
        }

        Restaurant restaurant = new Restaurant(
                request.getName().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                owner,
                request.getAddress().trim(),
                request.getCity().trim(),
                request.getState().trim(),
                request.getPincode().trim(),
                request.getPhone().trim(),
                request.getEmail().trim(),
                request.getCuisineType().trim(),
                request.getImageUrl() != null ? request.getImageUrl().trim() : null,
                request.getOpeningTime(),
                request.getClosingTime()
        );

        restaurant.setStatus(RestaurantStatus.PENDING);
        restaurant.setActive(true);

        Restaurant saved = restaurantRepository.save(restaurant);
        return RestaurantResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getOwnerRestaurants(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Owner account not found"));

        return restaurantRepository.findByOwnerId(owner.getId()).stream()
                .map(RestaurantResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getOwnerRestaurantById(String ownerEmail, Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));

        validateOwnership(restaurant, ownerEmail);

        return RestaurantResponse.fromEntity(restaurant);
    }

    @Transactional
    public RestaurantResponse updateRestaurant(String ownerEmail, Long id, RestaurantUpdateRequest request) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));

        validateOwnership(restaurant, ownerEmail);

        // Owners are only allowed to update operational details, never status or active directly here
        restaurant.setName(request.getName().trim());
        restaurant.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        restaurant.setAddress(request.getAddress().trim());
        restaurant.setCity(request.getCity().trim());
        restaurant.setState(request.getState().trim());
        restaurant.setPincode(request.getPincode().trim());
        restaurant.setPhone(request.getPhone().trim());
        restaurant.setEmail(request.getEmail().trim());
        restaurant.setCuisineType(request.getCuisineType().trim());
        restaurant.setImageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null);
        restaurant.setOpeningTime(request.getOpeningTime());
        restaurant.setClosingTime(request.getClosingTime());

        Restaurant updated = restaurantRepository.save(restaurant);
        return RestaurantResponse.fromEntity(updated);
    }

    @Transactional
    public RestaurantResponse deactivateRestaurantByOwner(String ownerEmail, Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));

        validateOwnership(restaurant, ownerEmail);

        restaurant.setActive(false);
        Restaurant saved = restaurantRepository.save(restaurant);
        return RestaurantResponse.fromEntity(saved);
    }

    // ==========================================
    // ADMIN FUNCTIONALITY
    // ==========================================

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getAllRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(RestaurantResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getPendingRestaurants() {
        return restaurantRepository.findByStatus(RestaurantStatus.PENDING).stream()
                .map(RestaurantResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurantByIdAdmin(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
        return RestaurantResponse.fromEntity(restaurant);
    }

    @Transactional
    public RestaurantResponse approveRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));

        restaurant.setStatus(RestaurantStatus.APPROVED);
        Restaurant saved = restaurantRepository.save(restaurant);
        return RestaurantResponse.fromEntity(saved);
    }

    @Transactional
    public RestaurantResponse rejectRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));

        restaurant.setStatus(RestaurantStatus.REJECTED);
        Restaurant saved = restaurantRepository.save(restaurant);
        return RestaurantResponse.fromEntity(saved);
    }

    @Transactional
    public RestaurantResponse activateRestaurantByAdmin(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));

        restaurant.setActive(true);
        Restaurant saved = restaurantRepository.save(restaurant);
        return RestaurantResponse.fromEntity(saved);
    }

    @Transactional
    public RestaurantResponse deactivateRestaurantByAdmin(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));

        restaurant.setActive(false);
        Restaurant saved = restaurantRepository.save(restaurant);
        return RestaurantResponse.fromEntity(saved);
    }

    // ==========================================
    // CUSTOMER / PUBLIC FUNCTIONALITY
    // ==========================================

    @Transactional(readOnly = true)
    public Page<RestaurantResponse> getApprovedActiveRestaurants(Pageable pageable) {
        return restaurantRepository.findByStatusAndActiveTrue(RestaurantStatus.APPROVED, pageable)
                .map(RestaurantResponse::fromEntityPublic);
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getApprovedRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findByIdAndStatusAndActiveTrue(id, RestaurantStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found or is currently not available"));

        return RestaurantResponse.fromEntityPublic(restaurant);
    }

    @Transactional(readOnly = true)
    public Page<RestaurantResponse> searchRestaurantsByName(String name, Pageable pageable) {
        return restaurantRepository.findByStatusAndActiveTrueAndNameContainingIgnoreCase(
                RestaurantStatus.APPROVED, name, pageable
        ).map(RestaurantResponse::fromEntityPublic);
    }

    @Transactional(readOnly = true)
    public Page<RestaurantResponse> filterRestaurantsByCity(String city, Pageable pageable) {
        return restaurantRepository.findByStatusAndActiveTrueAndCityIgnoreCase(
                RestaurantStatus.APPROVED, city, pageable
        ).map(RestaurantResponse::fromEntityPublic);
    }

    @Transactional(readOnly = true)
    public Page<RestaurantResponse> filterRestaurantsByCuisine(String cuisine, Pageable pageable) {
        return restaurantRepository.findByStatusAndActiveTrueAndCuisineTypeIgnoreCase(
                RestaurantStatus.APPROVED, cuisine, pageable
        ).map(RestaurantResponse::fromEntityPublic);
    }

    // Helper method
    private void validateOwnership(Restaurant restaurant, String ownerEmail) {
        if (!restaurant.getOwner().getEmail().equalsIgnoreCase(ownerEmail)) {
            throw new AccessDeniedException("Unauthorized: You do not own this restaurant");
        }
    }
}

