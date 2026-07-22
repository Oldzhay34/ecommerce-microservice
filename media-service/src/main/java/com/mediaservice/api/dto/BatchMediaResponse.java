package com.mediaservice.api.dto;

import java.util.List;
import java.util.Map;

public class BatchMediaResponse {

    private final Map<String, List<MediaAssetResponse>> items;

    public BatchMediaResponse(Map<String, List<MediaAssetResponse>> items) {
        this.items = items;
    }

    public Map<String, List<MediaAssetResponse>> getItems() {
        return items;
    }
}