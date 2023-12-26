package com.civitai.server.utils;

import lombok.Data;

@Data
public class CustomResponse<T> {
    private boolean success;
    private String message;
    private T payload;

    // Constructors, getters, and setters

    public CustomResponse(boolean success, String message, T payload) {
        this.success = success;
        this.message = message;
        this.payload = payload;
    }

    public static <T> CustomResponse<T> success(String message, T payload) {
        return new CustomResponse<>(true, message, payload);
    }

    public static <T> CustomResponse<T> success(String message) {
        return new CustomResponse<>(true, message, null);
    }

    public static <T> CustomResponse<T> failure(String message) {
        return new CustomResponse<>(false, message, null);
    }

}