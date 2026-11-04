package com.cravely.backend;

import com.cravely.backend.config.RazorpayProperties;
import com.cravely.backend.controller.RazorpayWebhookController;
import com.cravely.backend.dto.ApiResponse;
import com.cravely.backend.dto.VerifyPaymentRequest;
import com.cravely.backend.entity.Order;
import com.cravely.backend.entity.Payment;
import com.cravely.backend.entity.User;
import com.cravely.backend.repository.OrderRepository;
import com.cravely.backend.repository.PaymentRepository;
import com.cravely.backend.repository.UserRepository;
import com.cravely.backend.service.PaymentService;
import com.cravely.backend.service.RazorpayClientWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RazorpayOptionalConfigurationTest {

    @Autowired
    private RazorpayProperties razorpayProperties;

    @Autowired
    private RazorpayClientWrapper razorpayClientWrapper;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RazorpayWebhookController webhookController;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("RazorpayProperties correctly identifies unconfigured vs configured state")
    void testRazorpayPropertiesConfigurationCheck() {
        RazorpayProperties props = new RazorpayProperties();
        props.setKeyId("");
        props.setKeySecret("");
        props.setWebhookSecret("");

        assertFalse(props.isConfigured());
        assertFalse(props.isWebhookConfigured());

        props.setKeyId("rzp_test_placeholder_key_id");
        props.setKeySecret("placeholder_key_secret_12345");
        assertFalse(props.isConfigured(), "Placeholders should not be considered configured");

        props.setKeyId("rzp_test_validkey123");
        props.setKeySecret("validsecret12345678");
        props.setWebhookSecret("validwebhooksecret");
        assertTrue(props.isConfigured());
        assertTrue(props.isWebhookConfigured());
    }

    @Test
    @DisplayName("RazorpayClientWrapper generates mock order when credentials are not configured")
    void testCreateOrderWhenUnconfigured() {
        var orderDetails = razorpayClientWrapper.createOrder(50000L, "INR", "RCPT-TEST-001");
        assertNotNull(orderDetails);
        assertNotNull(orderDetails.id());
        assertTrue(orderDetails.id().startsWith("order_mock_"));
        assertEquals("INR", orderDetails.currency());
        assertEquals(50000L, orderDetails.amount());
    }

    @Test
    @DisplayName("RazorpayWebhookController returns 503 Service Unavailable when webhook secret is unconfigured")
    void testWebhookRejectedWhenUnconfigured() {
        ResponseEntity<ApiResponse<String>> response = webhookController.handleWebhook(
                "test-signature",
                "{\"event\":\"payment.captured\"}"
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("disabled"));
    }
}

