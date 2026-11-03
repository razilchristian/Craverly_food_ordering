package com.cravely.backend.dto;

import java.math.BigDecimal;

public class RazorpayOrderResponse {

    private String razorpayOrderId;
    private String razorpayKeyId;
    private Long amount; // in paise
    private BigDecimal amountInRupees;
    private String currency;
    private Long orderId;
    private String orderNumber;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    public RazorpayOrderResponse() {
    }

    public RazorpayOrderResponse(String razorpayOrderId, String razorpayKeyId, Long amount,
                                 BigDecimal amountInRupees, String currency, Long orderId,
                                 String orderNumber, String customerName, String customerEmail,
                                 String customerPhone) {
        this.razorpayOrderId = razorpayOrderId;
        this.razorpayKeyId = razorpayKeyId;
        this.amount = amount;
        this.amountInRupees = amountInRupees;
        this.currency = currency;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    public void setRazorpayKeyId(String razorpayKeyId) {
        this.razorpayKeyId = razorpayKeyId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public BigDecimal getAmountInRupees() {
        return amountInRupees;
    }

    public void setAmountInRupees(BigDecimal amountInRupees) {
        this.amountInRupees = amountInRupees;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }
}
