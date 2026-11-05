package com.cravely.backend.controller;

import com.cravely.backend.dto.*;
import com.cravely.backend.entity.OrderStatus;
import com.cravely.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping({"/api/orders", "/orders"})
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            Principal principal,
            @Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse order = orderService.placeOrder(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", order));
    }

    @GetMapping(value = {"", "/my-orders"})
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getMyOrders(
            Principal principal,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderSummaryResponse> orders = orderService.getMyOrders(principal.getName(), status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders fetched successfully", orders));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetails(
            Principal principal,
            @PathVariable Long orderId) {
        OrderResponse order = orderService.getCustomerOrderDetails(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order details fetched successfully", order));
    }

    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<ApiResponse<OrderTrackingResponse>> getOrderTracking(
            Principal principal,
            @PathVariable Long orderId) {
        OrderTrackingResponse tracking = orderService.getOrderTracking(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order tracking fetched successfully", tracking));
    }

    @GetMapping("/{orderId}/tracking/history")
    public ResponseEntity<ApiResponse<List<OrderStatusHistoryResponse>>> getOrderTrackingHistory(
            Principal principal,
            @PathVariable Long orderId) {
        List<OrderStatusHistoryResponse> history = orderService.getOrderTrackingHistory(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order tracking history fetched successfully", history));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            Principal principal,
            @PathVariable Long orderId,
            @Valid @RequestBody CancelOrderRequest request) {
        OrderResponse order = orderService.cancelCustomerOrder(principal.getName(), orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", order));
    }
}
