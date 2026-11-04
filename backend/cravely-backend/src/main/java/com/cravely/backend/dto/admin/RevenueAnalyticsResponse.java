package com.cravely.backend.dto.admin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RevenueAnalyticsResponse {

    private String period;
    private String currency = "INR";
    private BigDecimal totalRevenue;
    private BigDecimal deliveredRevenue;
    private BigDecimal reconciliationExposure;
    private List<RevenueDataPoint> data = new ArrayList<>();

    public RevenueAnalyticsResponse() {
        this.totalRevenue = BigDecimal.ZERO;
        this.deliveredRevenue = BigDecimal.ZERO;
        this.reconciliationExposure = BigDecimal.ZERO;
    }

    public static class RevenueDataPoint {
        private String label;
        private String date;
        private BigDecimal revenue;
        private long orders;

        public RevenueDataPoint() {
            this.revenue = BigDecimal.ZERO;
        }

        public RevenueDataPoint(String label, String date, BigDecimal revenue, long orders) {
            this.label = label;
            this.date = date;
            this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
            this.orders = orders;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
        }

        public long getOrders() {
            return orders;
        }

        public void setOrders(long orders) {
            this.orders = orders;
        }
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
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

    public List<RevenueDataPoint> getData() {
        return data;
    }

    public void setData(List<RevenueDataPoint> data) {
        this.data = data != null ? data : new ArrayList<>();
    }
}
