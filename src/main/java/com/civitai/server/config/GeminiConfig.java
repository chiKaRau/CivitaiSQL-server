package com.civitai.server.config;

import com.civitai.server.utils.ConfigUtils;
import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Bean
    public Client geminiClient() {

        ConfigUtils.loadConfig("GeminiConfig.json");
        String geminiApiKey = ConfigUtils.getConfigValue("apiKey");

        return Client.builder()
                .apiKey(geminiApiKey)
                .build();
    }
}