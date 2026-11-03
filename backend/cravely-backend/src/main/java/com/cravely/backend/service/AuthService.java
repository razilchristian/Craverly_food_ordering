package com.cravely.backend.service;

import com.cravely.backend.dto.ApiResponse;
import com.cravely.backend.dto.AuthResponse;
import com.cravely.backend.dto.LoginRequest;
import com.cravely.backend.dto.RegisterRequest;
import com.cravely.backend.dto.UserResponse;
import com.cravely.backend.entity.Role;
import com.cravely.backend.entity.User;
import com.cravely.backend.repository.UserRepository;
import com.cravely.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public ApiResponse<UserResponse> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }

        if (request.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Registration for ADMIN role is not permitted");
        }

        Role assignedRole = request.getRole() != null ? request.getRole() : Role.CUSTOMER;

        User user = new User(
                request.getFullName(),
                request.getEmail(),
                request.getPhone(),
                passwordEncoder.encode(request.getPassword()),
                assignedRole,
                true
        );

        User savedUser = userRepository.save(user);
        return ApiResponse.success("User registered successfully", UserResponse.fromEntity(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (Boolean.FALSE.equals(user.getActive())) {
            throw new DisabledException("Account is deactivated. Please contact support.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String token = jwtService.generateToken(user);
        return AuthResponse.of(true, "Login successful", token, user);
    }
}

