package com.civitai.server.models.dto;

import lombok.Data;
import java.util.List;

@Data
public class PendingAiSuggestionDTO {
    private String civitaiVersionID; // id
    private String characterName;

    private String aiSuggestedArtworkTitle; // Gemini title
    private String jikanNormalizedArtworkTitle; // Jikan normalized title

    private List<String> aiSuggestedDownloadFilePath; // fuzzy matches for Gemini title
    private List<String> jikanSuggestedDownloadFilePath; // fuzzy matches for Jikan title

    private List<String> localSuggestedDownloadFilePath; // you’ll fill later
}
