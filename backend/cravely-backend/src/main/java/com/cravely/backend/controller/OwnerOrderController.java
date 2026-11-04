package com.cravely.backend.controller;

import com.cravely.backend.dto.*;
import com.cravely.backend.entity.OrderStatus;
import com.cravely.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasRole('RESTAURANT_OWNER')")
public class OwnerOrderController {

    private final OrderService orderService;

    public OwnerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/restaurants/{restaurantId}/orders")
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getRestaurantOrders(
            Principal principal,
            @PathVariable Long restaurantId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderSummaryResponse> orders = orderService.getRestaurantOrders(principal.getName(), restaurantId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Restaurant orders fetched successfully", orders));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOwnerOrderDetails(
            Principal principal,
            @PathVariable Long orderId) {
        OrderResponse order = orderService.getOwnerOrderDetails(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order details fetched successfully", order));
    }

    @GetMapping("/orders/{orderId}/tracking")
    public ResponseEntity<ApiResponse<OrderTrackingResponse>> getOwnerOrderTracking(
            Principal principal,
            @PathVariable Long orderId) {
        OrderTrackingResponse tracking = orderService.getOwnerOrderTracking(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order tracking fetched successfully", tracking));
    }

    @PatchMapping("/orders/{orderId}/accept")
    public ResponseEntity<ApiResponse<OrderResponse>> acceptOrder(
            Principal principal,
            @PathVariable Long orderId) {
        OrderResponse order = orderService.acceptOrder(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order accepted successfully", order));
    }

    @PatchMapping("/orders/{orderId}/reject")
    public ResponseEntity<ApiResponse<OrderResponse>> rejectOrder(
            Principal principal,
            @PathVariable Long orderId,
            @Valid @RequestBody RejectOrderRequest request) {
        OrderResponse order = orderService.rejectOrder(principal.getName(), orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Order rejected successfully", order));
    }

    @PatchMapping("/orders/{orderId}/preparing")
    public ResponseEntity<ApiResponse<OrderResponse>> markPreparing(
            Principal principal,
            @PathVariable Long orderId) {
        OrderResponse order = orderService.markOrderPreparing(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order marked as preparing", order));
    }

    @PatchMapping("/orders/{orderId}/ready")
    public ResponseEntity<ApiResponse<OrderResponse>> markReady(
            Principal principal,
            @PathVariable Long orderId) {
        OrderResponse order = orderService.markOrderReady(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order marked as ready for pickup", order));
    }

    @PatchMapping("/orders/{orderId}/out-for-delivery")
    public ResponseEntity<ApiResponse<OrderResponse>> markOutForDelivery(
            Principal principal,
            @PathVariable Long orderId) {
        OrderResponse order = orderService.markOrderOutForDeliveryByOwner(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order marked as out for delivery", order));
    }

    @PatchMapping("/orders/{orderId}/delivered")
    public ResponseEntity<ApiResponse<OrderResponse>> markDelivered(
            Principal principal,
            @PathVariable Long orderId) {
        OrderResponse order = orderService.markOrderDeliveredByOwner(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order marked as delivered", order));
    }
}
