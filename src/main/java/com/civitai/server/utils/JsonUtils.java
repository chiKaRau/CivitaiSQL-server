package com.civitai.server.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {

    private JsonUtils() {
        // Private constructor to prevent instantiation
    }

    public static <T> String convertObjectToString(T value) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            // Handle the exception as needed
            e.printStackTrace();
            return null;
        }
    }

    public static <T> T convertStringToObject(String jsonString, Class<T> valueType) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(jsonString, valueType);
        } catch (JsonProcessingException e) {
            // Handle the exception as needed
            e.printStackTrace();
            return null;
        }
    }
}
