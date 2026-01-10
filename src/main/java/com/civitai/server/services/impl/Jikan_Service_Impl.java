package com.civitai.server.services.impl;

import com.civitai.server.services.Jikan_Service;
import com.civitai.server.services.impl.Gemini_Service_impl.CharacterTitleMatch;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class Jikan_Service_Impl implements Jikan_Service {

    private static final String JIKAN_BASE = "https://api.jikan.moe/v4";
    private static final int SEARCH_LIMIT = 8;

    /**
     * Minimal “don’t spam public API” guard:
     * - at most 2 requests/sec
     * - plus in-memory cache
     */
    private static final long MIN_INTERVAL_MS = 550; // ~1.8 req/sec

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Very simple in-memory cache (good enough for a server)
    // key: "ro:<query>" or "jp:<query>"
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private volatile long lastCallAtMs = 0;

    public Jikan_Service_Impl(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<CharacterTitleMatch> normalizeTitles(List<CharacterTitleMatch> aiSuggestion) {
        if (aiSuggestion == null || aiSuggestion.isEmpty())
            return List.of();

        // 1) Collect unique titles to resolve (skip UNKNOWN/blank)
        List<String> uniqueTitles = aiSuggestion.stream()
                .filter(Objects::nonNull)
                .map(CharacterTitleMatch::getTitle)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> !"UNKNOWN".equalsIgnoreCase(s))
                .distinct()
                .collect(Collectors.toList());

        if (uniqueTitles.isEmpty())
            return aiSuggestion;

        // 2) Resolve each unique title once
        Map<String, String> resolvedMap = new HashMap<>();
        for (String t : uniqueTitles) {
            // If you want ONLY anime normalization, you can add heuristics here.
            // I recommend trying resolution and falling back if not found.
            String romaji = resolveRomajiTitle(t);
            if (romaji != null && !romaji.isBlank()) {
                resolvedMap.put(t, romaji);
            }
        }

        // 3) Produce new list preserving order
        List<CharacterTitleMatch> out = new ArrayList<>(aiSuggestion.size());
        for (CharacterTitleMatch m : aiSuggestion) {
            if (m == null)
                continue;

            CharacterTitleMatch copy = new CharacterTitleMatch();
            copy.setId(m.getId());
            copy.setCharacterName(m.getCharacterName());

            String title = m.getTitle();
            if (title != null) {
                String trimmed = title.trim();
                String resolved = resolvedMap.get(trimmed);
                copy.setTitle(resolved != null ? resolved : trimmed);
            } else {
                copy.setTitle(null);
            }

            out.add(copy);
        }
        return out;
    }

    @Override
    public String resolveRomajiTitle(String rawTitle) {
        return resolveTitle(rawTitle, false);
    }

    @Override
    public String resolveJapaneseTitle(String rawTitle) {
        return resolveTitle(rawTitle, true);
    }

    // ---------------- Core resolver ----------------

    private String resolveTitle(String rawTitle, boolean preferJapaneseChars) {
        if (rawTitle == null)
            return null;

        String query = rawTitle.trim();
        if (query.isBlank())
            return null;

        // cache
        String cacheKey = (preferJapaneseChars ? "jp:" : "ro:") + query;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }

        // Try anime first
        String found = searchJikan("anime", query, preferJapaneseChars);

        // Fallback to manga
        if (found == null) {
            found = searchJikan("manga", query, preferJapaneseChars);
        }

        // If still null => store short negative cache to reduce repeated calls
        if (found == null) {
            cache.put(cacheKey, CacheEntry.negative(Duration.ofHours(6))); // tune as you like
            return null;
        }

        cache.put(cacheKey, CacheEntry.positive(found, Duration.ofDays(7)));
        return found;
    }

    private String searchJikan(String type, String query, boolean preferJapaneseChars) {
        // be nice to public API
        rateLimit();

        String q = UriUtils.encodeQueryParam(query, StandardCharsets.UTF_8);
        String url = JIKAN_BASE + "/" + type + "?q=" + q + "&limit=" + SEARCH_LIMIT + "&sfw";

        try {
            String body = restTemplate.getForObject(url, String.class);
            if (body == null || body.isBlank())
                return null;

            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            if (!data.isArray() || data.size() == 0)
                return null;

            // Pick best match among top N results
            String qNorm = norm(query);
            int bestScore = Integer.MIN_VALUE;
            JsonNode best = null;

            for (JsonNode item : data) {
                int score = scoreCandidate(item, qNorm);
                if (score > bestScore) {
                    bestScore = score;
                    best = item;
                }
            }

            if (best == null)
                return null;

            // Safety: if score is too low, treat as “not confident”
            if (bestScore < 80) {
                return null;
            }

            if (preferJapaneseChars) {
                String jp = best.path("title_japanese").asText("");
                if (!jp.isBlank())
                    return jp;
            }

            // Default title is typically romaji for anime
            String def = best.path("title").asText("");
            return def.isBlank() ? null : def;

        } catch (Exception e) {
            return null;
        }
    }

    // ---------------- Matching / scoring ----------------

    private int scoreCandidate(JsonNode item, String qNorm) {
        List<String> cands = new ArrayList<>();

        add(cands, text(item, "title"));
        add(cands, text(item, "title_english"));
        add(cands, text(item, "title_japanese"));

        // title_synonyms: array
        JsonNode syn = item.path("title_synonyms");
        if (syn.isArray()) {
            for (JsonNode s : syn)
                add(cands, s.asText(null));
        }

        // titles: array of {type,title}
        JsonNode titles = item.path("titles");
        if (titles.isArray()) {
            for (JsonNode t : titles)
                add(cands, t.path("title").asText(null));
        }

        int best = Integer.MIN_VALUE;
        for (String c : cands) {
            int s = scoreString(c, qNorm);
            if (s > best)
                best = s;
        }
        return best;
    }

    private int scoreString(String candidate, String qNorm) {
        if (candidate == null || candidate.isBlank())
            return Integer.MIN_VALUE;
        String cNorm = norm(candidate);

        if (cNorm.equals(qNorm))
            return 1000;
        if (cNorm.contains(qNorm))
            return 850;
        if (qNorm.contains(cNorm))
            return 700;

        // token overlap
        Set<String> qt = tokens(qNorm);
        Set<String> ct = tokens(cNorm);
        int overlap = 0;
        for (String t : qt)
            if (ct.contains(t))
                overlap++;

        // bonus if overlap covers most query tokens
        int coverageBonus = (qt.isEmpty() ? 0 : (overlap * 100 / qt.size()));

        return overlap * 60 + coverageBonus;
    }

    private static String norm(String s) {
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static Set<String> tokens(String s) {
        if (s == null || s.isBlank())
            return Set.of();
        return new HashSet<>(Arrays.asList(s.split(" ")));
    }

    private static void add(List<String> out, String s) {
        if (s != null && !s.isBlank())
            out.add(s);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText(null);
    }

    // ---------------- Rate limiting ----------------

    private void rateLimit() {
        long now = System.currentTimeMillis();
        long wait = (lastCallAtMs + MIN_INTERVAL_MS) - now;
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ignored) {
            }
        }
        lastCallAtMs = System.currentTimeMillis();
    }

    // ---------------- Cache entry ----------------

    private static class CacheEntry {
        final String value; // null means negative cache
        final long expiresAtMs;

        private CacheEntry(String value, long expiresAtMs) {
            this.value = value;
            this.expiresAtMs = expiresAtMs;
        }

        static CacheEntry positive(String value, Duration ttl) {
            return new CacheEntry(value, System.currentTimeMillis() + ttl.toMillis());
        }

        static CacheEntry negative(Duration ttl) {
            return new CacheEntry(null, System.currentTimeMillis() + ttl.toMillis());
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMs;
        }
    }
}
