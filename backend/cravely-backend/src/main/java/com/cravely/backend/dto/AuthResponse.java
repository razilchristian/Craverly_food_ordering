package com.cravely.backend.dto;

import com.cravely.backend.entity.Role;
import com.cravely.backend.entity.User;

public class AuthResponse {

    private boolean success;
    private String message;
    private String token;
    private UserInfo user;

    public AuthResponse() {
    }

    public AuthResponse(boolean success, String message, String token, UserInfo user) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.user = user;
    }

    public static AuthResponse of(boolean success, String message, String token, User user) {
        UserInfo userInfo = user != null
                ? new UserInfo(user.getId(), user.getFullName(), user.getEmail(), user.getRole())
                : null;
        return new AuthResponse(success, message, token, userInfo);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public static class UserInfo {
        private Long id;
        private String fullName;
        private String email;
        private Role role;

        public UserInfo() {
        }

        public UserInfo(Long id, String fullName, String email, Role role) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }
    }
}

