package com.mediaservice.infrastructure.persistence.entity;

import com.mediaservice.domain.model.MediaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity - persistence katmani. Domain modeli (MediaAsset) bu siniftan tamamen bagimsizdir.
 * Tablo/kolon isimleri snake_case; sinif/alan isimleri CamelCase.
 */
@Entity
@Table(name = "media_asset")
public class MediaAssetJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    @Column(name = "thumb_url", nullable = false, length = 1024)
    private String thumbUrl;

    @Column(name = "original_url", nullable = false, length = 1024)
    private String originalUrl;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "storage_key_thumb", nullable = false, length = 512)
    private String storageKeyThumb;

    @Column(name = "storage_key_medium", nullable = false, length = 512)
    private String storageKeyMedium;

    @Column(name = "storage_key_original", nullable = false, length = 512)
    private String storageKeyOriginal;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private MediaStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public MediaAssetJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public void setStoreId(UUID storeId) {
        this.storeId = storeId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public void setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getStorageKeyThumb() {
        return storageKeyThumb;
    }

    public void setStorageKeyThumb(String storageKeyThumb) {
        this.storageKeyThumb = storageKeyThumb;
    }

    public String getStorageKeyMedium() {
        return storageKeyMedium;
    }

    public void setStorageKeyMedium(String storageKeyMedium) {
        this.storageKeyMedium = storageKeyMedium;
    }

    public String getStorageKeyOriginal() {
        return storageKeyOriginal;
    }

    public void setStorageKeyOriginal(String storageKeyOriginal) {
        this.storageKeyOriginal = storageKeyOriginal;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public MediaStatus getStatus() {
        return status;
    }

    public void setStatus(MediaStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}