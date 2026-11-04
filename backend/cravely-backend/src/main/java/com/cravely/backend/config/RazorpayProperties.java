package com.cravely.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RazorpayProperties {

    @Value("${razorpay.key.id:}")
    private String keyId;

    @Value("${razorpay.key.secret:}")
    private String keySecret;

    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    @Value("${razorpay.currency:INR}")
    private String currency;

    /**
     * Returns true only when valid, non-placeholder credentials are provided.
     */
    public boolean isConfigured() {
        return keyId != null && !keyId.trim().isEmpty() && !keyId.contains("placeholder")
                && keySecret != null && !keySecret.trim().isEmpty() && !keySecret.contains("placeholder");
    }

    /**
     * Returns true only when a valid, non-placeholder webhook secret is provided.
     */
    public boolean isWebhookConfigured() {
        return webhookSecret != null && !webhookSecret.trim().isEmpty() && !webhookSecret.contains("placeholder");
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeySecret() {
        return keySecret;
    }

    public void setKeySecret(String keySecret) {
        this.keySecret = keySecret;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getCurrency() {
        return (currency != null && !currency.trim().isEmpty()) ? currency : "INR";
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
