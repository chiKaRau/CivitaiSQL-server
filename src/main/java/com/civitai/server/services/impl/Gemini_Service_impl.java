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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
                // // Build a small test batch (you can change to 100 items easily)
                // List<CharacterMatchInput> inputs = List.of(
                // // simple
                // input("0", "uzumaki naruto",
                // List.of("konoha", "ninja"),
                // null,
                // null),

                // // simple
                // input("1", "luffy",
                // List.of("straw hat"),
                // null,
                // null),

                // // closer to your real example
                // input("2", "Reiko Kujirai - Kowloon Generic Romance - 14 Outfits -
                // Illustrious/SDXL",
                // List.of("kowloon generic romance", "reiko kujirai", "anime",
                // "character"),
                // "KGRReiko-ILXL.safetensors",
                // "https://civitai.com/models/2283121/reiko-kujirai-kowloon-generic-romance-14-outfits-illustrioussdxl"));

                // List<CharacterTitleMatch> matches = matchCharactersToTitles(inputs);

                // // Pretty print result
                // System.out.println("Gemini Connection Successful! Matches:");
                // System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(matches));

                // } catch (Exception e) {
                // System.err.println("Gemini Connection Failed! Check your API Key. Error: " +
                // e.getMessage());
                // e.printStackTrace();
                // }
                System.out.println("--- Gemini API Connection Test Finished ---");
        }

        // helper to reduce boilerplate
        private static CharacterMatchInput input(
                        String id,
                        String characterName,
                        List<String> tags,
                        String civitaiFileName,
                        String civitaiUrl) {
                CharacterMatchInput in = new CharacterMatchInput();
                in.setId(id);
                in.setCharacterName(characterName);
                in.setTags(tags);
                in.setCivitaiFileName(civitaiFileName);
                in.setCivitaiUrl(civitaiUrl);
                return in;
        }

        // TODO
        // if found engnlish? return the romaji of the title

        public List<CharacterTitleMatch> matchCharactersToTitles(List<CharacterMatchInput> inputs) {
                if (inputs == null || inputs.isEmpty()) {
                        return List.of();
                }

                // 1) Normalize + assign stable IDs (so we can match outputs safely)
                List<CharacterMatchInput> normalized = new ArrayList<>();
                int idx = 0;

                for (CharacterMatchInput in : inputs) {
                        if (in == null)
                                continue;

                        String name = safeTrim(in.getCharacterName());
                        if (name.isBlank())
                                continue;

                        CharacterMatchInput n = new CharacterMatchInput();
                        n.setId(in.getId() != null && !in.getId().isBlank() ? in.getId().trim() : String.valueOf(idx));
                        n.setCharacterName(name);

                        // Keep only short, high-signal tags (optional)
                        List<String> tags = in.getTags() == null ? List.of()
                                        : in.getTags().stream()
                                                        .filter(Objects::nonNull)
                                                        .map(String::trim)
                                                        .filter(s -> s.length() >= 2)
                                                        .distinct()
                                                        .limit(25)
                                                        .collect(Collectors.toList());
                        n.setTags(tags);

                        n.setCivitaiFileName(safeTrimToNull(in.getCivitaiFileName()));
                        n.setCivitaiUrl(safeTrimToNull(in.getCivitaiUrl()));

                        normalized.add(n);
                        idx++;
                }

                if (normalized.isEmpty()) {
                        return List.of();
                }

                // 2) Output schema: [{ id, characterName, title }, ...]
                Schema itemSchema = Schema.builder()
                                .type(Type.Known.OBJECT)
                                .properties(Map.of(
                                                "id", Schema.builder().type(Type.Known.STRING).build(),
                                                "characterName", Schema.builder().type(Type.Known.STRING).build(),
                                                "title", Schema.builder().type(Type.Known.STRING).build()))
                                .required("id", "characterName", "title")
                                .build();

                Schema responseSchema = Schema.builder()
                                .type(Type.Known.ARRAY)
                                .items(itemSchema)
                                .build();

                GenerateContentConfig config = GenerateContentConfig.builder()
                                .safetySettings(List.of(
                                                SafetySetting.builder()
                                                                .category(HarmCategory.Known.HARM_CATEGORY_SEXUALLY_EXPLICIT)
                                                                .threshold(HarmBlockThreshold.Known.OFF)
                                                                .build(),
                                                SafetySetting.builder()
                                                                .category(HarmCategory.Known.HARM_CATEGORY_DANGEROUS_CONTENT)
                                                                .threshold(HarmBlockThreshold.Known.OFF)
                                                                .build()))
                                .responseMimeType("application/json")
                                .responseSchema(responseSchema)
                                .candidateCount(1)
                                .build();

                // 3) Build prompt with JSON input
                String inputJson;
                try {
                        inputJson = objectMapper.writeValueAsString(normalized);
                } catch (Exception e) {
                        throw new RuntimeException("Failed to serialize inputs to JSON.", e);
                }

                String prompt = "You will be given a JSON array of objects describing characters.\n" +
                                "Your job: for each item, identify the most likely canonical franchise/series title.\n"
                                +
                                "\n" +
                                "IMPORTANT TITLE FORMAT:\n" +
                                "- If the franchise is Japanese anime/manga/light novel/visual novel, output the PRIMARY ROMANIZED JAPANESE TITLE (ROMAJI),\n"
                                +
                                "  as commonly listed on MyAnimeList (myanimelist.net).\n" +
                                "  Examples:\n" +
                                "    Demon Slayer -> Kimetsu no Yaiba\n" +
                                "    Assassination Classroom -> Ansatsu Kyoushitsu\n" +
                                "    Attack on Titan -> Shingeki no Kyojin\n" +
                                "    Pretty Cure -> Precure\n" +
                                "- If the franchise is NOT Japanese (e.g., Transformers), output the common English franchise title.\n"
                                +
                                "\n" +
                                "Rules:\n" +
                                "1) Output MUST be valid JSON matching the response schema (array of objects).\n" +
                                "2) Output array length MUST equal input length and preserve the same order.\n" +
                                "3) Copy back the same 'id' and 'characterName' exactly as provided.\n" +
                                "4) Use tags/fileName/url as hints.\n" +
                                "5) Do NOT include season/version tokens (e.g., V1, S2, 2019), authors, studios, or extra commentary.\n"
                                +
                                "6) If you truly cannot determine, set title to \"UNKNOWN\".\n" +
                                "\n" +
                                "Input:\n" + inputJson;

                String json = client.models.generateContent(MODEL, prompt, config).text();

                // 4) Parse JSON
                List<CharacterTitleMatch> parsed = parseJsonArray(json);

                // 5) Safety: if model returned wrong length, fallback to id-map merge
                if (parsed.size() != normalized.size()) {
                        Map<String, CharacterTitleMatch> byId = parsed.stream()
                                        .filter(x -> x != null && x.getId() != null)
                                        .collect(Collectors.toMap(CharacterTitleMatch::getId, x -> x, (a, b) -> a));

                        List<CharacterTitleMatch> fixed = new ArrayList<>();
                        for (CharacterMatchInput in : normalized) {
                                CharacterTitleMatch m = byId.get(in.getId());
                                if (m == null) {
                                        fixed.add(new CharacterTitleMatch(in.getId(), in.getCharacterName(),
                                                        "UNKNOWN"));
                                } else {
                                        // ensure characterName is exactly input
                                        m.setCharacterName(in.getCharacterName());
                                        fixed.add(m);
                                }
                        }
                        return fixed;
                }

                // Ensure exact characterName echo
                for (int i = 0; i < parsed.size(); i++) {
                        parsed.get(i).setCharacterName(normalized.get(i).getCharacterName());
                        parsed.get(i).setId(normalized.get(i).getId());
                }

                return parsed;
        }

        // ---------- Helpers ----------

        private List<CharacterTitleMatch> parseJsonArray(String json) {
                try {
                        return objectMapper.readValue(json, new TypeReference<List<CharacterTitleMatch>>() {
                        });
                } catch (Exception parseEx) {
                        // If any stray text sneaks in, slice the first [ ... ]
                        int start = json.indexOf('[');
                        int end = json.lastIndexOf(']');
                        if (start >= 0 && end > start) {
                                String sliced = json.substring(start, end + 1);
                                try {
                                        return objectMapper.readValue(sliced,
                                                        new TypeReference<List<CharacterTitleMatch>>() {
                                                        });
                                } catch (Exception ignored) {
                                }
                        }
                        throw new IllegalStateException("Gemini returned non-JSON output: " + json, parseEx);
                }
        }

        private static String safeTrim(String s) {
                return s == null ? "" : s.trim();
        }

        private static String safeTrimToNull(String s) {
                if (s == null)
                        return null;
                String t = s.trim();
                return t.isBlank() ? null : t;
        }

        // ---------- DTOs ----------

        public static class CharacterMatchInput {
                private String id; // optional; if null we fill it
                private String characterName; // required
                private List<String> tags; // optional
                private String civitaiFileName; // optional
                private String civitaiUrl; // optional

                public CharacterMatchInput() {
                }

                public String getId() {
                        return id;
                }

                public void setId(String id) {
                        this.id = id;
                }

                public String getCharacterName() {
                        return characterName;
                }

                public void setCharacterName(String characterName) {
                        this.characterName = characterName;
                }

                public List<String> getTags() {
                        return tags;
                }

                public void setTags(List<String> tags) {
                        this.tags = tags;
                }

                public String getCivitaiFileName() {
                        return civitaiFileName;
                }

                public void setCivitaiFileName(String civitaiFileName) {
                        this.civitaiFileName = civitaiFileName;
                }

                public String getCivitaiUrl() {
                        return civitaiUrl;
                }

                public void setCivitaiUrl(String civitaiUrl) {
                        this.civitaiUrl = civitaiUrl;
                }
        }

        public static class CharacterTitleMatch {
                private String id;
                private String characterName;
                private String title;

                public CharacterTitleMatch() {
                }

                public CharacterTitleMatch(String id, String characterName, String title) {
                        this.id = id;
                        this.characterName = characterName;
                        this.title = title;
                }

                public String getId() {
                        return id;
                }

                public void setId(String id) {
                        this.id = id;
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
