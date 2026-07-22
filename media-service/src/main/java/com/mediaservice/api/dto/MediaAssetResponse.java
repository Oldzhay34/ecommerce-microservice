package com.mediaservice.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Redis'e JSON olarak serilestirildigi icin no-arg + setter degil,
 * @JsonCreator ile immutable deserialization kullanilir.
 */
public class MediaAssetResponse {

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

    @JsonCreator
    public MediaAssetResponse(@JsonProperty("assetId") UUID assetId,
                              @JsonProperty("productId") UUID productId,
                              @JsonProperty("url") String url,
                              @JsonProperty("thumbUrl") String thumbUrl,
                              @JsonProperty("originalUrl") String originalUrl,
                              @JsonProperty("contentType") String contentType,
                              @JsonProperty("sizeBytes") long sizeBytes,
                              @JsonProperty("width") int width,
                              @JsonProperty("height") int height,
                              @JsonProperty("sortOrder") int sortOrder,
                              @JsonProperty("primary") boolean primary,
                              @JsonProperty("createdAt") Instant createdAt) {
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