package com.civitai.server.services.impl;

import com.civitai.server.services.Gemini_Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.HarmBlockThreshold;
import com.google.genai.types.HarmCategory;
import com.google.genai.types.SafetySetting;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class Gemini_Service_impl implements Gemini_Service {

        private static final String MODEL = "gemini-robotics-er-1.5-preview";
        // If your chosen model complains about responseSchema/JSON, switch to a
        // mainline model
        // like "gemini-2.5-flash". :contentReference[oaicite:1]{index=1}

        private final Client client;
        private final ObjectMapper objectMapper;

        public Gemini_Service_impl(Client client, ObjectMapper objectMapper) {
                this.client = client;
                this.objectMapper = objectMapper;
        }

        @PostConstruct
        public void initTest() {
                System.out.println("--- Starting Gemini API Connection Test ---");
                // try {
                //         List<String> characters = List.of("uzumaki naruto", "luffy");
                //         List<CharacterTitleMatch> result = matchCharactersToTitles(characters);

                //         // Print as JSON
                //         System.out.println("Gemini Connection Successful! Test Result: "
                //                         + objectMapper.writeValueAsString(result));
                // } catch (Exception e) {
                //         System.err.println("Gemini Connection Failed! Check your API Key. Error: " + e.getMessage());
                //         e.printStackTrace();
                // }
                System.out.println("--- Gemini API Connection Test Finished ---");
        }

        @Override
        public List<CharacterTitleMatch> matchCharactersToTitles(List<String> characterNames) {
                if (characterNames == null || characterNames.isEmpty()) {
                        return List.of();
                }

                // JSON schema: [{ "characterName": "...", "title": "..." }, ...]
                Schema schema = Schema.builder()
                                .type(Type.Known.ARRAY)
                                .items(
                                                Schema.builder()
                                                                .type(Type.Known.OBJECT)
                                                                .properties(
                                                                                Map.of(
                                                                                                "characterName",
                                                                                                Schema.builder().type(
                                                                                                                Type.Known.STRING)
                                                                                                                .build(),
                                                                                                "title",
                                                                                                Schema.builder().type(
                                                                                                                Type.Known.STRING)
                                                                                                                .build()))
                                                                .required("characterName", "title")
                                                                .build())
                                .build();

                GenerateContentConfig config = GenerateContentConfig.builder()
                                // Keep your safety settings
                                .safetySettings(List.of(
                                                SafetySetting.builder()
                                                                .category(HarmCategory.Known.HARM_CATEGORY_SEXUALLY_EXPLICIT)
                                                                .threshold(HarmBlockThreshold.Known.OFF)
                                                                .build(),
                                                SafetySetting.builder()
                                                                .category(HarmCategory.Known.HARM_CATEGORY_DANGEROUS_CONTENT)
                                                                .threshold(HarmBlockThreshold.Known.OFF)
                                                                .build()))
                                // Force JSON output that matches the schema
                                // :contentReference[oaicite:2]{index=2}
                                .responseMimeType("application/json")
                                .responseSchema(schema)
                                .candidateCount(1)
                                .build();

                String charactersJson;
                try {
                        charactersJson = objectMapper.writeValueAsString(characterNames);
                } catch (Exception e) {
                        throw new RuntimeException("Failed to serialize characterNames to JSON.", e);
                }

                String prompt = "You will be given a JSON array of character names.\n" +
                                "For each character, return the most well-known canonical franchise/series title.\n" +
                                "Rules:\n" +
                                "1) Output MUST be valid JSON matching the response schema.\n" +
                                "2) Output array length must equal input length and keep the same order.\n" +
                                "3) characterName must match the input string exactly.\n" +
                                "4) If you truly cannot determine, set title to \"UNKNOWN\".\n\n" +
                                "Characters:\n" + charactersJson;

                String json = client.models.generateContent(MODEL, prompt, config).text();

                // Parse into Java list
                try {
                        return objectMapper.readValue(json, new TypeReference<List<CharacterTitleMatch>>() {
                        });
                } catch (Exception parseEx) {
                        // Small fallback: if the model ever wraps JSON with extra text, try slicing [
                        // ... ]
                        int start = json.indexOf('[');
                        int end = json.lastIndexOf(']');
                        if (start >= 0 && end > start) {
                                String sliced = json.substring(start, end + 1);
                                try {
                                        return objectMapper.readValue(sliced,
                                                        new TypeReference<List<CharacterTitleMatch>>() {
                                                        });
                                } catch (Exception ignored) {
                                        // fall through
                                }
                        }
                        throw new IllegalStateException("Gemini returned non-JSON output: " + json, parseEx);
                }
        }

        // Java 11 friendly POJO (no records)
        public static class CharacterTitleMatch {
                private String characterName;
                private String title;

                public CharacterTitleMatch() {
                }

                public CharacterTitleMatch(String characterName, String title) {
                        this.characterName = characterName;
                        this.title = title;
                }

                public String getCharacterName() {
                        return characterName;
                }

                public void setCharacterName(String characterName) {
                        this.characterName = characterName;
                }

                public String getTitle() {
                        return title;
                }

                public void setTitle(String title) {
                        this.title = title;
                }
        }
}
