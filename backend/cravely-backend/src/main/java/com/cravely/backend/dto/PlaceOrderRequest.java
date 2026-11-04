package com.cravely.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PlaceOrderRequest {

    @NotBlank(message = "Delivery address is required")
    @Size(max = 500, message = "Delivery address cannot exceed 500 characters")
    private String deliveryAddress;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String deliveryCity;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String deliveryState;

    @NotBlank(message = "Pincode is required")
    @Size(max = 20, message = "Pincode cannot exceed 20 characters")
    private String deliveryPincode;

    @NotBlank(message = "Phone number is required")
    @Size(max = 30, message = "Phone number cannot exceed 30 characters")
    private String customerPhone;

    @Size(max = 500, message = "Customer note cannot exceed 500 characters")
    private String customerNote;

    @Size(max = 50, message = "Coupon code cannot exceed 50 characters")
    private String couponCode;

    public PlaceOrderRequest() {
    }

    public PlaceOrderRequest(String deliveryAddress, String deliveryCity, String deliveryState,
                             String deliveryPincode, String customerPhone, String customerNote) {
        this.deliveryAddress = deliveryAddress;
        this.deliveryCity = deliveryCity;
        this.deliveryState = deliveryState;
        this.deliveryPincode = deliveryPincode;
        this.customerPhone = customerPhone;
        this.customerNote = customerNote;
    }

    public PlaceOrderRequest(String deliveryAddress, String deliveryCity, String deliveryState,
                             String deliveryPincode, String customerPhone, String customerNote, String couponCode) {
        this(deliveryAddress, deliveryCity, deliveryState, deliveryPincode, customerPhone, customerNote);
        this.couponCode = couponCode;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getDeliveryCity() {
        return deliveryCity;
    }

    public void setDeliveryCity(String deliveryCity) {
        this.deliveryCity = deliveryCity;
    }

    public String getDeliveryState() {
        return deliveryState;
    }

    public void setDeliveryState(String deliveryState) {
        this.deliveryState = deliveryState;
    }

    public String getDeliveryPincode() {
        return deliveryPincode;
    }

    public void setDeliveryPincode(String deliveryPincode) {
        this.deliveryPincode = deliveryPincode;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerNote() {
        return customerNote;
    }

    public void setCustomerNote(String customerNote) {
        this.customerNote = customerNote;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}

