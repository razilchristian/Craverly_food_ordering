package com.cravely.backend.dto.admin;

import com.cravely.backend.dto.OrderSummaryResponse;

import java.util.ArrayList;
import java.util.List;

public class OrderAnalyticsResponse {

    private long totalOrders;
    private long placedOrders;
    private long acceptedOrders;
    private long preparingOrders;
    private long readyForPickupOrders;
    private long outForDeliveryOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long rejectedOrders;
    private long activeOrders;
    private List<OrderSummaryResponse> recentOrders = new ArrayList<>();

    public OrderAnalyticsResponse() {
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getPlacedOrders() {
        return placedOrders;
    }

    public void setPlacedOrders(long placedOrders) {
        this.placedOrders = placedOrders;
    }

    public long getAcceptedOrders() {
        return acceptedOrders;
    }

    public void setAcceptedOrders(long acceptedOrders) {
        this.acceptedOrders = acceptedOrders;
    }

    public long getPreparingOrders() {
        return preparingOrders;
    }

    public void setPreparingOrders(long preparingOrders) {
        this.preparingOrders = preparingOrders;
    }

    public long getReadyForPickupOrders() {
        return readyForPickupOrders;
    }

    public void setReadyForPickupOrders(long readyForPickupOrders) {
        this.readyForPickupOrders = readyForPickupOrders;
    }

    public long getOutForDeliveryOrders() {
        return outForDeliveryOrders;
    }

    public void setOutForDeliveryOrders(long outForDeliveryOrders) {
        this.outForDeliveryOrders = outForDeliveryOrders;
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

    public long getActiveOrders() {
        return activeOrders;
    }

    public void setActiveOrders(long activeOrders) {
        this.activeOrders = activeOrders;
    }

    public List<OrderSummaryResponse> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<OrderSummaryResponse> recentOrders) {
        this.recentOrders = recentOrders != null ? recentOrders : new ArrayList<>();
    }
}
