package com.cravely.backend.dto;

import jakarta.validation.constraints.NotNull;

public class SimulatePaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    public SimulatePaymentRequest() {
    }

    public SimulatePaymentRequest(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}

