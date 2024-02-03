package com.civitai.server.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigUtils {
    private static Map<String, Object> configMap = new HashMap<>();

    @SuppressWarnings("null")
    public static void loadConfig(String configFileName) {
        try {
            ClassPathResource resource = new ClassPathResource(configFileName);
            ObjectMapper mapper = new ObjectMapper();

            // Load the configuration file into the map
            configMap = mapper.readValue(resource.getInputStream(), new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String getConfigValue(String key) {
        Object value = configMap.get(key);
        return value != null ? value.toString() : null;
    }
}
