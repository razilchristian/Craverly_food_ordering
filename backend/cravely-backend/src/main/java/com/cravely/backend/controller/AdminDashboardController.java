package com.cravely.backend.controller;

import com.cravely.backend.dto.ApiResponse;
import com.cravely.backend.dto.admin.*;
import com.cravely.backend.service.AdminDashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getOverview() {
        DashboardOverviewResponse overview = dashboardService.getOverview();
        return ResponseEntity.ok(ApiResponse.success("Dashboard overview fetched successfully", overview));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueAnalyticsResponse>> getRevenueAnalytics(
            @RequestParam(required = false, defaultValue = "monthly") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        RevenueAnalyticsResponse revenue = dashboardService.getRevenueAnalytics(period, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Revenue analytics fetched successfully", revenue));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<OrderAnalyticsResponse>> getOrderAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        OrderAnalyticsResponse orders = dashboardService.getOrderAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Order analytics fetched successfully", orders));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<UserAnalyticsResponse>> getUserAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UserAnalyticsResponse users = dashboardService.getUserAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("User analytics fetched successfully", users));
    }

    @GetMapping("/restaurants")
    public ResponseEntity<ApiResponse<RestaurantAnalyticsResponse>> getRestaurantAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        RestaurantAnalyticsResponse restaurants = dashboardService.getRestaurantAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Restaurant analytics fetched successfully", restaurants));
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<ApiResponse<List<RecentActivityResponse>>> getRecentActivity(
            @RequestParam(defaultValue = "20") int limit) {
        List<RecentActivityResponse> activity = dashboardService.getRecentActivity(limit);
        return ResponseEntity.ok(ApiResponse.success("Recent activity fetched successfully", activity));
    }
}
