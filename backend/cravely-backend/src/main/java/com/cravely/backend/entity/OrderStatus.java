package com.cravely.backend.entity;

public enum OrderStatus {
    PLACED,
    ACCEPTED,
    REJECTED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}
