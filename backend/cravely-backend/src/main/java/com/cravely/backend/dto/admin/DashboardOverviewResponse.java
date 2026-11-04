package com.cravely.backend.dto.admin;

import java.math.BigDecimal;

public class DashboardOverviewResponse {

    private long totalUsers;
    private long totalCustomers;
    private long totalRestaurantOwners;
    private long totalAdmins;
    private long activeUsers;
    private long deactivatedUsers;

    private long totalRestaurants;
    private long pendingRestaurants;
    private long approvedRestaurants;
    private long rejectedRestaurants;
    private long activeRestaurants;

    private long totalFoodItems;
    private long availableFoodItems;

    private long totalOrders;
    private long pendingOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long rejectedOrders;

    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private BigDecimal deliveredRevenue;
    private BigDecimal reconciliationExposure;

    public DashboardOverviewResponse() {
        this.totalRevenue = BigDecimal.ZERO;
        this.todayRevenue = BigDecimal.ZERO;
        this.deliveredRevenue = BigDecimal.ZERO;
        this.reconciliationExposure = BigDecimal.ZERO;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getTotalRestaurantOwners() {
        return totalRestaurantOwners;
    }

    public void setTotalRestaurantOwners(long totalRestaurantOwners) {
        this.totalRestaurantOwners = totalRestaurantOwners;
    }

    public long getTotalAdmins() {
        return totalAdmins;
    }

    public void setTotalAdmins(long totalAdmins) {
        this.totalAdmins = totalAdmins;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getDeactivatedUsers() {
        return deactivatedUsers;
    }

    public void setDeactivatedUsers(long deactivatedUsers) {
        this.deactivatedUsers = deactivatedUsers;
    }

    public long getTotalRestaurants() {
        return totalRestaurants;
    }

    public void setTotalRestaurants(long totalRestaurants) {
        this.totalRestaurants = totalRestaurants;
    }

    public long getPendingRestaurants() {
        return pendingRestaurants;
    }

    public void setPendingRestaurants(long pendingRestaurants) {
        this.pendingRestaurants = pendingRestaurants;
    }

    public long getApprovedRestaurants() {
        return approvedRestaurants;
    }

    public void setApprovedRestaurants(long approvedRestaurants) {
        this.approvedRestaurants = approvedRestaurants;
    }

    public long getRejectedRestaurants() {
        return rejectedRestaurants;
    }

    public void setRejectedRestaurants(long rejectedRestaurants) {
        this.rejectedRestaurants = rejectedRestaurants;
    }

    public long getActiveRestaurants() {
        return activeRestaurants;
    }

    public void setActiveRestaurants(long activeRestaurants) {
        this.activeRestaurants = activeRestaurants;
    }

    public long getTotalFoodItems() {
        return totalFoodItems;
    }

    public void setTotalFoodItems(long totalFoodItems) {
        this.totalFoodItems = totalFoodItems;
    }

    public long getAvailableFoodItems() {
        return availableFoodItems;
    }

    public void setAvailableFoodItems(long availableFoodItems) {
        this.availableFoodItems = availableFoodItems;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getDeliveredOrders() {
        return deliveredOrders;
    }

    public void setDeliveredOrders(long deliveredOrders) {
        this.deliveredOrders = deliveredOrders;
    }

    public long getCancelledOrders() {
        return cancelledOrders;
    }

    public void setCancelledOrders(long cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }

    public long getRejectedOrders() {
        return rejectedOrders;
    }

    public void setRejectedOrders(long rejectedOrders) {
        this.rejectedOrders = rejectedOrders;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
    }

    public BigDecimal getTodayRevenue() {
        return todayRevenue;
    }

    public void setTodayRevenue(BigDecimal todayRevenue) {
        this.todayRevenue = todayRevenue != null ? todayRevenue : BigDecimal.ZERO;
    }

    public BigDecimal getDeliveredRevenue() {
        return deliveredRevenue;
    }

    public void setDeliveredRevenue(BigDecimal deliveredRevenue) {
        this.deliveredRevenue = deliveredRevenue != null ? deliveredRevenue : BigDecimal.ZERO;
    }

    public BigDecimal getReconciliationExposure() {
        return reconciliationExposure;
    }

    public void setReconciliationExposure(BigDecimal reconciliationExposure) {
        this.reconciliationExposure = reconciliationExposure != null ? reconciliationExposure : BigDecimal.ZERO;
    }
}
