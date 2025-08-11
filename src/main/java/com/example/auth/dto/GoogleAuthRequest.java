package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class GoogleAuthRequest {
    
    @NotBlank(message = "El token de Google es requerido")
    private String token;

    
    public GoogleAuthRequest() {}

    
    public GoogleAuthRequest(String token) {
        this.token = token;
    }

    
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "GoogleAuthRequest{" +
                "token='" + token + '\'' +
                '}';
    }
}
