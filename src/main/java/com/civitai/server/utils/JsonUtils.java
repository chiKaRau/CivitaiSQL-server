package com.civitai.server.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

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

    public static String[] extractImageUrls(Map<String, Object> jsonObject) {
        // Get the images list from the map
        List<Map<String, Object>> imagesList = (List<Map<String, Object>>) jsonObject.get("images");

        // Create an array to store the URLs
        String[] imageUrls = new String[imagesList.size()];

        // Iterate over the images list and extract the URLs
        for (int i = 0; i < imagesList.size(); i++) {
            Map<String, Object> imageObject = imagesList.get(i);
            imageUrls[i] = (String) imageObject.get("url");
        }

        // Return the array of URLs
        return imageUrls;
    }

}
