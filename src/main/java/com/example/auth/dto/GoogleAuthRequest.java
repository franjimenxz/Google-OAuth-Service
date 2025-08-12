package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class GoogleAuthRequest {

    @NotBlank(message = "El código de autorización de Google es requerido")
    private String code;


    public GoogleAuthRequest() {}


    public GoogleAuthRequest(String code) {
        this.code = code;
    }


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "GoogleAuthRequest{" +
                "code='" + code + '\'' +
                '}';
    }
}

