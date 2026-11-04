package com.cravely.backend.dto;

import com.cravely.backend.entity.OrderStatus;
import com.cravely.backend.entity.OrderStatusHistory;

import java.time.LocalDateTime;

public class OrderStatusHistoryResponse {

    private Long id;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private String changedBy;
    private String changedByRole;
    private LocalDateTime changedAt;
    private String remarks;

    public OrderStatusHistoryResponse() {
    }

    public static OrderStatusHistoryResponse fromEntity(OrderStatusHistory history) {
        if (history == null) {
            return null;
        }
        OrderStatusHistoryResponse dto = new OrderStatusHistoryResponse();
        dto.setId(history.getId());
        dto.setPreviousStatus(history.getPreviousStatus());
        dto.setNewStatus(history.getNewStatus());
        if (history.getChangedBy() != null) {
            dto.setChangedBy(history.getChangedBy().getFullName());
            dto.setChangedByRole(history.getChangedBy().getRole() != null ? history.getChangedBy().getRole().name() : null);
        } else {
            dto.setChangedBy("System");
            dto.setChangedByRole("SYSTEM");
        }
        dto.setChangedAt(history.getChangedAt());
        dto.setRemarks(history.getRemarks());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(OrderStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(OrderStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getChangedByRole() {
        return changedByRole;
    }

    public void setChangedByRole(String changedByRole) {
        this.changedByRole = changedByRole;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}

