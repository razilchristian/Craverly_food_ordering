package com.cravely.backend.controller;

import com.cravely.backend.dto.*;
import com.cravely.backend.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasRole('CUSTOMER')")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> createOrder(
            Principal principal,
            @Valid @RequestBody CreatePaymentOrderRequest request) {
        RazorpayOrderResponse response = paymentService.createRazorpayOrder(principal.getName(), request.getOrderId());
        return ResponseEntity.ok(ApiResponse.success("Razorpay order created successfully", response));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            Principal principal,
            @Valid @RequestBody VerifyPaymentRequest request) {
        PaymentResponse response = paymentService.verifyPayment(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified and captured successfully", response));
    }

    @PostMapping("/retry/{orderId}")
    public ResponseEntity<ApiResponse<RazorpayOrderResponse>> retryPayment(
            Principal principal,
            @PathVariable Long orderId) {
        RazorpayOrderResponse response = paymentService.retryPayment(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Payment retry initiated successfully", response));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentHistory(
            Principal principal,
            @PathVariable Long orderId) {
        List<PaymentResponse> history = paymentService.getPaymentHistoryByOrder(principal.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Payment history fetched successfully", history));
    }
}
