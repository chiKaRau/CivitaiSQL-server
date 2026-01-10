package com.civitai.server.services;

import java.util.List;

public interface Jikan_Service {

    /**
     * Normalize titles using Jikan (anime first, then manga).
     * Keeps original title if not confidently resolvable.
     *
     * @param aiSuggestion list returned by Gemini
     * @return new list (same order) with normalized titles
     */
    List<com.civitai.server.services.impl.Gemini_Service_impl.CharacterTitleMatch> normalizeTitles(
            List<com.civitai.server.services.impl.Gemini_Service_impl.CharacterTitleMatch> aiSuggestion);

    /**
     * Resolve a single raw title into canonical romaji (Default title on
     * MAL/Jikan),
     * or return null if not found.
     */
    String resolveRomajiTitle(String rawTitle);

    /**
     * Resolve a single raw title into Japanese characters, or return null if not
     * found.
     */
    String resolveJapaneseTitle(String rawTitle);
}