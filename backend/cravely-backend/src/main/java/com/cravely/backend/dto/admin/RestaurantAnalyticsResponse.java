package com.cravely.backend.dto.admin;

import java.util.ArrayList;
import java.util.List;

public class RestaurantAnalyticsResponse {

    private long totalRestaurants;
    private long pendingApprovals;
    private long approvedRestaurants;
    private long rejectedRestaurants;
    private long activeRestaurants;
    private long inactiveRestaurants;
    private List<CuisineDistribution> cuisineDistribution = new ArrayList<>();

    public RestaurantAnalyticsResponse() {
    }

    public static class CuisineDistribution {
        private String cuisine;
        private long count;

        public CuisineDistribution() {
        }

        public CuisineDistribution(String cuisine, long count) {
            this.cuisine = cuisine;
            this.count = count;
        }

        public String getCuisine() {
            return cuisine;
        }

        public void setCuisine(String cuisine) {
            this.cuisine = cuisine;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public long getTotalRestaurants() {
        return totalRestaurants;
    }

    public void setTotalRestaurants(long totalRestaurants) {
        this.totalRestaurants = totalRestaurants;
    }

    public long getPendingApprovals() {
        return pendingApprovals;
    }

    public void setPendingApprovals(long pendingApprovals) {
        this.pendingApprovals = pendingApprovals;
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

    public long getInactiveRestaurants() {
        return inactiveRestaurants;
    }

    public void setInactiveRestaurants(long inactiveRestaurants) {
        this.inactiveRestaurants = inactiveRestaurants;
    }

    public List<CuisineDistribution> getCuisineDistribution() {
        return cuisineDistribution;
    }

    public void setCuisineDistribution(List<CuisineDistribution> cuisineDistribution) {
        this.cuisineDistribution = cuisineDistribution != null ? cuisineDistribution : new ArrayList<>();
    }
}

