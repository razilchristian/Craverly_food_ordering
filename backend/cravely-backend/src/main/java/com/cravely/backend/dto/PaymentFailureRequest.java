package com.cravely.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class PaymentFailureRequest {

    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;

    private String razorpayPaymentId;
    private String errorCode;
    private String errorDescription;
    private String errorReason;

    public PaymentFailureRequest() {
    }

    public PaymentFailureRequest(String razorpayOrderId, String razorpayPaymentId,
                                 String errorCode, String errorDescription, String errorReason) {
        this.razorpayOrderId = razorpayOrderId;
        this.razorpayPaymentId = razorpayPaymentId;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.errorReason = errorReason;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }
}

