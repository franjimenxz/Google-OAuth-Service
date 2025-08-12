package com.example.auth.dto;

import com.example.auth.model.User;

public class AuthResponse {
    private boolean success;
    private String message;
    private User user;

    
    public AuthResponse() {}

    
    public AuthResponse(boolean success, String message, User user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    
    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.user = null;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "AuthResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", user=" + user +
                '}';
    }
}

