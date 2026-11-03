package com.cravely.backend.controller;

import com.cravely.backend.dto.ApiResponse;
import com.cravely.backend.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/webhook")
public class RazorpayWebhookController {

    private final PaymentService paymentService;

    public RazorpayWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> handleWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signatureHeader,
            @RequestBody String rawPayload) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Missing X-Razorpay-Signature header"));
        }

        paymentService.processWebhook(rawPayload, signatureHeader);
        return ResponseEntity.ok(ApiResponse.success("Webhook processed successfully", "OK"));
    }
}
