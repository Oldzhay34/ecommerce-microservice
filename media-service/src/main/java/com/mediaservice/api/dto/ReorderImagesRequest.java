package com.mediaservice.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public class ReorderImagesRequest {

    @NotEmpty(message = "assetIds bos olamaz")
    private List<UUID> assetIds;

    public ReorderImagesRequest() {
    }

    public ReorderImagesRequest(List<UUID> assetIds) {
        this.assetIds = assetIds;
    }

    public List<UUID> getAssetIds() {
        return assetIds;
    }

    public void setAssetIds(List<UUID> assetIds) {
        this.assetIds = assetIds;
    }
}