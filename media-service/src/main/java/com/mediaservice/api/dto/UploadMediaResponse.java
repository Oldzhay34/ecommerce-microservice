package com.mediaservice.api.dto;

import java.time.Instant;
import java.util.UUID;

public class UploadMediaResponse {

    private final UUID assetId;
    private final UUID productId;
    private final String url;
    private final String thumbUrl;
    private final String originalUrl;
    private final String contentType;
    private final long sizeBytes;
    private final int width;
    private final int height;
    private final int sortOrder;
    private final boolean primary;
    private final Instant createdAt;

    public UploadMediaResponse(UUID assetId,
                               UUID productId,
                               String url,
                               String thumbUrl,
                               String originalUrl,
                               String contentType,
                               long sizeBytes,
                               int width,
                               int height,
                               int sortOrder,
                               boolean primary,
                               Instant createdAt) {
        this.assetId = assetId;
        this.productId = productId;
        this.url = url;
        this.thumbUrl = thumbUrl;
        this.originalUrl = originalUrl;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.sortOrder = sortOrder;
        this.primary = primary;
        this.createdAt = createdAt;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getUrl() {
        return url;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isPrimary() {
        return primary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}