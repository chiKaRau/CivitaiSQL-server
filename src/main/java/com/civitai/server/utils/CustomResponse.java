package com.civitai.server.utils;

import lombok.Data;

@Data
public class CustomResponse<T> {
    private boolean success;
    private String message;
    private T data;

    // Constructors, getters, and setters

    public CustomResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> CustomResponse<T> success(String message, T data) {
        return new CustomResponse<>(true, message, data);
    }

    public static <T> CustomResponse<T> success(String message) {
        return new CustomResponse<>(true, message, null);
    }

    public static <T> CustomResponse<T> failure(String message) {
        return new CustomResponse<>(false, message, null);
    }

}