package com.cravely.backend.dto;

public class RestaurantApprovalRequest {

    private String reason;

    public RestaurantApprovalRequest() {
    }

    public RestaurantApprovalRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

