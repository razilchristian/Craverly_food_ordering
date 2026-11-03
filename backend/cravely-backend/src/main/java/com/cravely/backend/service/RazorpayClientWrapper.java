package com.cravely.backend.service;

import com.cravely.backend.config.RazorpayProperties;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RazorpayClientWrapper {

    private final RazorpayProperties razorpayProperties;

    public RazorpayClientWrapper(RazorpayProperties razorpayProperties) {
        this.razorpayProperties = razorpayProperties;
    }

    public record RazorpayOrderDetails(String id, String currency, long amount, String status) {}
    public record RazorpayPaymentDetails(String id, String status, String method) {}

    public RazorpayOrderDetails createOrder(long amountInPaise, String currency, String receipt) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayProperties.getKeyId(), razorpayProperties.getKeySecret());
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency != null ? currency : razorpayProperties.getCurrency());
            orderRequest.put("receipt", receipt);

            com.razorpay.Order order = client.orders.create(orderRequest);
            return new RazorpayOrderDetails(
                    order.get("id"),
                    order.get("currency"),
                    ((Number) order.get("amount")).longValue(),
                    order.get("status")
            );
        } catch (Exception e) {
            // Safe development placeholder fallback
            if (razorpayProperties.getKeyId() == null || razorpayProperties.getKeyId().contains("placeholder")) {
                String mockOrderId = "order_mock_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000, 9999);
                return new RazorpayOrderDetails(mockOrderId, currency != null ? currency : "INR", amountInPaise, "created");
            }
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }

    public RazorpayPaymentDetails fetchPayment(String razorpayPaymentId) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayProperties.getKeyId(), razorpayProperties.getKeySecret());
            com.razorpay.Payment payment = client.payments.fetch(razorpayPaymentId);
            String status = payment.get("status");
            String method = payment.has("method") ? String.valueOf(payment.get("method")) : "test";
            return new RazorpayPaymentDetails(razorpayPaymentId, status, method);
        } catch (Exception e) {
            if (razorpayProperties.getKeyId() == null || razorpayProperties.getKeyId().contains("placeholder")) {
                return new RazorpayPaymentDetails(razorpayPaymentId, "captured", "upi");
            }
            throw new RuntimeException("Failed to fetch Razorpay payment: " + e.getMessage(), e);
        }
    }

    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String signature) {
        if (razorpayOrderId == null || razorpayPaymentId == null || signature == null) {
            return false;
        }
        String data = razorpayOrderId + "|" + razorpayPaymentId;
        return verifyHmacSha256(data, razorpayProperties.getKeySecret(), signature);
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        if (payload == null || signature == null) {
            return false;
        }
        return verifyHmacSha256(payload, razorpayProperties.getWebhookSecret(), signature);
    }

    public String computePaymentSignature(String razorpayOrderId, String razorpayPaymentId) {
        String data = razorpayOrderId + "|" + razorpayPaymentId;
        return calculateHmacSha256(data, razorpayProperties.getKeySecret());
    }

    public String computeWebhookSignature(String payload) {
        return calculateHmacSha256(payload, razorpayProperties.getWebhookSecret());
    }

    private boolean verifyHmacSha256(String data, String secret, String expectedSignatureHex) {
        try {
            String calculatedHex = calculateHmacSha256(data, secret);
            return MessageDigest.isEqual(
                    calculatedHex.toLowerCase().getBytes(StandardCharsets.UTF_8),
                    expectedSignatureHex.trim().toLowerCase().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    private String calculateHmacSha256(String data, String secret) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC SHA256 calculation failed: " + e.getMessage(), e);
        }
    }
}
