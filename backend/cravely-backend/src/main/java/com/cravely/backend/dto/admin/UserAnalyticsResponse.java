package com.cravely.backend.dto.admin;

import com.cravely.backend.dto.UserResponse;

import java.util.ArrayList;
import java.util.List;

public class UserAnalyticsResponse {

    private long totalUsers;
    private long customers;
    private long restaurantOwners;
    private long admins;
    private long activeUsers;
    private long deactivatedUsers;
    private long newUsersInPeriod;
    private List<UserResponse> recentUsers = new ArrayList<>();

    public UserAnalyticsResponse() {
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getCustomers() {
        return customers;
    }

    public void setCustomers(long customers) {
        this.customers = customers;
    }

    public long getRestaurantOwners() {
        return restaurantOwners;
    }

    public void setRestaurantOwners(long restaurantOwners) {
        this.restaurantOwners = restaurantOwners;
    }

    public long getAdmins() {
        return admins;
    }

    public void setAdmins(long admins) {
        this.admins = admins;
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

    public long getNewUsersInPeriod() {
        return newUsersInPeriod;
    }

    public void setNewUsersInPeriod(long newUsersInPeriod) {
        this.newUsersInPeriod = newUsersInPeriod;
    }

    public List<UserResponse> getRecentUsers() {
        return recentUsers;
    }

    public void setRecentUsers(List<UserResponse> recentUsers) {
        this.recentUsers = recentUsers != null ? recentUsers : new ArrayList<>();
    }
}
