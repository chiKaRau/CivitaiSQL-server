package com.civitai.server.models.dto;

import java.util.List;

public class PageResponse<T> {
    public List<T> content;
    public int page; // 0-based
    public int size;
    public long totalElements;
    public int totalPages;
    public boolean hasNext;
    public boolean hasPrevious;
}