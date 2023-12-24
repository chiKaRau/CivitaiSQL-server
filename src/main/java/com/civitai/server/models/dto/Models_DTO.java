package com.civitai.server.models.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class Models_DTO {

    private String name;

    private List<String> tags;

    private String url;

    private String category;

    private String versionNumber;

    private String modelNumber;

    private List<String> triggerWords;

    private String description;

    private String type;

    private String stats;

    private LocalDate uploaded;

    private String baseModel;

    private String hash;

    private String usageTips;

    private String creatorName;

    private Boolean nsfw;

    private Boolean flag;

    private List<Map<String, Object>> imageUrls;

}
