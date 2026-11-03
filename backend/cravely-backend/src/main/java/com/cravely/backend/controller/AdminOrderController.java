package com.cravely.backend.controller;

import com.cravely.backend.dto.ApiResponse;
import com.cravely.backend.dto.OrderResponse;
import com.cravely.backend.dto.OrderSummaryResponse;
import com.cravely.backend.entity.OrderStatus;
import com.cravely.backend.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderSummaryResponse> orders = orderService.getAllOrders(status, orderNumber, customer, restaurantId, pageable);
        return ResponseEntity.ok(ApiResponse.success("All orders fetched successfully", orders));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getAdminOrderDetails(@PathVariable Long orderId) {
        OrderResponse order = orderService.getAdminOrderDetails(orderId);
        return ResponseEntity.ok(ApiResponse.success("Order details fetched successfully", order));
    }
}
