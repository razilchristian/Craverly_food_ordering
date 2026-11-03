package com.cravely.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RazorpayProperties {

    @Value("${razorpay.key.id:rzp_test_placeholder_key_id}")
    private String keyId;

    @Value("${razorpay.key.secret:placeholder_key_secret_12345}")
    private String keySecret;

    @Value("${razorpay.webhook.secret:placeholder_webhook_secret_12345}")
    private String webhookSecret;

    @Value("${razorpay.currency:INR}")
    private String currency;

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
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
