package com.civitai.server.models.dto;

import lombok.Data;
import java.util.List;

@Data
public class TopTagsRequest {
    private int page = 0; // 0-based
    private int size = 100; // cap in service (<=500)
    private String source = "all"; // all|tags|fileName|titles|other

    private List<String> exclude; // lowercased in service
    private int minLen = 3;
    private boolean allowNumbers = false;

    // Optional: narrow which pending rows we count over
    private String search; // same meaning as list paging
    private String op = "contains"; // contains|does not contain|equals|does not equal|begins with|ends with
}