package com.cravely.backend;

import com.cravely.backend.dto.AuthResponse;
import com.cravely.backend.dto.LoginRequest;
import com.cravely.backend.dto.RegisterRequest;
import com.cravely.backend.entity.Role;
import com.cravely.backend.entity.User;
import com.cravely.backend.repository.UserRepository;
import com.cravely.backend.security.JwtService;
import com.cravely.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void testRegisterCustomerAndVerifyPasswordHashed() {
        String testEmail = "testuser" + System.currentTimeMillis() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest(
                "Test User",
                testEmail,
                "1234567890",
                "Password123!",
                Role.CUSTOMER
        );

        var response = authService.register(registerRequest);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData().getId());

        User savedUser = userRepository.findByEmail(testEmail).orElseThrow();
        assertNotEquals("Password123!", savedUser.getPassword());
        assertTrue(passwordEncoder.matches("Password123!", savedUser.getPassword()));
        assertEquals(Role.CUSTOMER, savedUser.getRole());
    }

    @Test
    void testAdminRegistrationRejection() {
        RegisterRequest registerRequest = new RegisterRequest(
                "Admin Attempt",
                "adminattempt@example.com",
                "1234567890",
                "Password123!",
                Role.ADMIN
        );

        assertThrows(IllegalArgumentException.class, () -> authService.register(registerRequest));
    }

    @Test
    void testLoginAndJwtGeneration() {
        String testEmail = "logintest" + System.currentTimeMillis() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest(
                "Login Tester",
                testEmail,
                "9876543210",
                "SecretPass123",
                Role.CUSTOMER
        );
        authService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest(testEmail, "SecretPass123");
        AuthResponse authResponse = authService.login(loginRequest);

        assertTrue(authResponse.isSuccess());
        assertNotNull(authResponse.getToken());
        assertTrue(jwtService.validateToken(authResponse.getToken()));
        assertEquals(testEmail, jwtService.extractUsername(authResponse.getToken()));
    }
}

