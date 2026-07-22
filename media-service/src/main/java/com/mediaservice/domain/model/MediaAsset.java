package com.mediaservice.domain.model;


import java.time.Instant;
import java.util.UUID;

/**
 * Domain modeli - SAF POJO. @Entity/@Table yok, Lombok yok.
 * storeId: [Karar E] ROLE_STORE tasiyan kullanicinin JWT 'sub' claim'indeki userId'sidir.
 * Ayri bir storeId attribute'u sistemde yoktur; Product servisiyle isim uyumu icin alan adi
 * storeId olarak korunmustur.
 */
public class MediaAsset {

    private UUID id;
    private UUID productId;
    private UUID storeId;
    private String url;
    private String thumbUrl;
    private String originalUrl;
    private String storageKey;
    private String storageKeyThumb;
    private String storageKeyMedium;
    private String storageKeyOriginal;
    private String contentType;
    private long sizeBytes;
    private int width;
    private int height;
    private int sortOrder;
    private boolean primary;
    private MediaStatus status;
    private Instant createdAt;
    private Long version;

    public MediaAsset() {
    }

    public MediaAsset(UUID id,
                      UUID productId,
                      UUID storeId,
                      String url,
                      String thumbUrl,
                      String originalUrl,
                      String storageKey,
                      String storageKeyThumb,
                      String storageKeyMedium,
                      String storageKeyOriginal,
                      String contentType,
                      long sizeBytes,
                      int width,
                      int height,
                      int sortOrder,
                      boolean primary,
                      MediaStatus status,
                      Instant createdAt,
                      Long version) {
        this.id = id;
        this.productId = productId;
        this.storeId = storeId;
        this.url = url;
        this.thumbUrl = thumbUrl;
        this.originalUrl = originalUrl;
        this.storageKey = storageKey;
        this.storageKeyThumb = storageKeyThumb;
        this.storageKeyMedium = storageKeyMedium;
        this.storageKeyOriginal = storageKeyOriginal;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.sortOrder = sortOrder;
        this.primary = primary;
        this.status = status;
        this.createdAt = createdAt;
        this.version = version;
    }

    /**
     * Invaryant: url alani .webp ile bitmiyorsa kayit gecersizdir.
     */
    public void validateWebpInvariant() {
        requireWebp(this.url, "url");
        requireWebp(this.thumbUrl, "thumbUrl");
        requireWebp(this.originalUrl, "originalUrl");
        if (!"image/webp".equals(this.contentType)) {
            throw new IllegalStateException("MediaAsset.contentType her zaman image/webp olmalidir");
        }
    }

    private void requireWebp(String value, String field) {
        if (value == null || !value.endsWith(".webp")) {
            throw new IllegalStateException("MediaAsset." + field + " .webp ile bitmelidir: " + value);
        }
    }

    /**
     * Soft delete. [Karar 3] Silinen asset primary bayragini da birakir; boylece
     * uq_media_asset_primary kismi indeksi ve domain temizligi birlikte korunur.
     */
    public void markDeleted() {
        this.status = MediaStatus.DELETED;
        this.primary = false;
    }

    public boolean isActive() {
        return MediaStatus.ACTIVE == this.status;
    }

    public boolean isOwnedBy(UUID candidateUserId) {
        return this.storeId != null && this.storeId.equals(candidateUserId);
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