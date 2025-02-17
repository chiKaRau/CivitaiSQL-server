package com.civitai.server.services.impl;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.civitai.server.exception.CustomException;
import com.civitai.server.services.Civitai_Service;

@Service
public class Civitai_Service_Impl implements Civitai_Service {

    private final RestTemplate restTemplate;
    private static final Logger log = LoggerFactory.getLogger(CivitaiSQL_Service_Impl.class);

    public Civitai_Service_Impl() {
        // Initialize RestTemplate with UTF-8 encoding
        this.restTemplate = new RestTemplate();
        this.restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }

    //Check interface class for CIVITAI_API
    @SuppressWarnings("unchecked")
    @Override
    public Optional<List<String>> findModelTagsByModelID(String modelID) {
        try {

            // Fetch the Tags using restTemplate
            Map<String, Object> response = restTemplate.getForObject(CIVITAI_MODELS_ENDPOINT + modelID, Map.class);

            // Check if the response is null
            if (response == null || response.containsKey("message")) {
                // Handle the error case
                log.error("API request failed: {}", response != null ? response.get("message") : "Response is null");
                return Optional.empty();
            }
            // Assuming the response structure is like { "tags": ["tag1", "tag2", ...] }
            List<String> tags = ((List<String>) response.get("tags"))
                    .stream()
                    .collect(Collectors.toList());

            // Check if the suggestionTags is null or empty
            return tags != null && !tags.isEmpty()
                    ? Optional.of(tags)
                    : Optional.empty();

        } catch (Exception e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while finding all records from models_table", e);
            throw new CustomException("An unexpected error occurred", e);
            // Alternatively, return a fallback response for less critical errors
            // return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<Map<String, Object>> findModelByModelID(String modelID) {
        try {

            // Fetch the Tags using restTemplate
            Map<String, Object> response = restTemplate.getForObject(CIVITAI_MODELS_ENDPOINT + modelID, Map.class);

            // Check if the response is null
            if (response == null || response.containsKey("message")) {
                // Handle the error case
                log.error("API request failed: {}", response != null ? response.get("message") : "Response is null");
                return Optional.empty();
            }

            return response != null && !response.isEmpty()
                    ? Optional.of(response)
                    : Optional.empty();

        } catch (Exception e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while finding all records from models_table", e);
            throw new CustomException("An unexpected error occurred", e);
            // Alternatively, return a fallback response for less critical errors
            // return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<Map<String, Object>> findModelByVersionID(String versionID) {
        try {

            System.out.println("Calling findModelByVersionID");

            // Fetch the Tags using restTemplate
            Map<String, Object> response = restTemplate.getForObject(CIVITAI_MODELS_VERSION_ENDPOINT + versionID,
                    Map.class);

            // Check if the response is null
            if (response == null || response.containsKey("message")) {
                // Handle the error case
                log.error("API request failed: {}", response != null ? response.get("message") : "Response is null");
                return Optional.empty();
            }

            return response != null && !response.isEmpty()
                    ? Optional.of(response)
                    : Optional.empty();

        } catch (Exception e) {
            // Log and handle other types of exceptions
            log.error("Unexpected error while finding all records from models_table", e);
            throw new CustomException("An unexpected error occurred", e);
            // Alternatively, return a fallback response for less critical errors
            // return Collections.emptyList();
        }
    }

}
