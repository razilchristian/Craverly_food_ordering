package com.cravely.backend.repository;

import com.cravely.backend.entity.Order;
import com.cravely.backend.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    Page<Order> findByCustomerIdAndOrderStatus(Long customerId, OrderStatus orderStatus, Pageable pageable);

    Page<Order> findByCustomerIdAndCreatedAtBetween(Long customerId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Order> findByCustomerIdAndOrderStatusAndCreatedAtBetween(
            Long customerId, OrderStatus orderStatus, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Order> findByRestaurantId(Long restaurantId, Pageable pageable);

    Page<Order> findByRestaurantIdAndOrderStatus(Long restaurantId, OrderStatus orderStatus, Pageable pageable);
}

