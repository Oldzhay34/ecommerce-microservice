package com.mediaservice.infrastructure.persistence.mapper;

import com.mediaservice.domain.model.MediaAsset;
import com.mediaservice.infrastructure.persistence.entity.MediaAssetJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain <-> JpaEntity manuel donusum. MapStruct/Lombok yok.
 */
@Component
public class MediaEntityMapper {

    public MediaAsset toDomain(MediaAssetJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MediaAsset(
                entity.getId(),
                entity.getProductId(),
                entity.getStoreId(),
                entity.getUrl(),
                entity.getThumbUrl(),
                entity.getOriginalUrl(),
                entity.getStorageKey(),
                entity.getStorageKeyThumb(),
                entity.getStorageKeyMedium(),
                entity.getStorageKeyOriginal(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getWidth(),
                entity.getHeight(),
                entity.getSortOrder(),
                entity.isPrimary(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getVersion()
        );
    }

    public MediaAssetJpaEntity toEntity(MediaAsset domain) {
        if (domain == null) {
            return null;
        }
        MediaAssetJpaEntity entity = new MediaAssetJpaEntity();
        applyToEntity(domain, entity);
        entity.setVersion(domain.getVersion());
        return entity;
    }

    /**
     * Managed entity uzerine domain degerlerini basar. version alani JPA tarafindan
     * yonetildigi icin BURADA set EDILMEZ.
     */
    public void applyToEntity(MediaAsset domain, MediaAssetJpaEntity entity) {
        entity.setId(domain.getId());
        entity.setProductId(domain.getProductId());
        entity.setStoreId(domain.getStoreId());
        entity.setUrl(domain.getUrl());
        entity.setThumbUrl(domain.getThumbUrl());
        entity.setOriginalUrl(domain.getOriginalUrl());
        entity.setStorageKey(domain.getStorageKey());
        entity.setStorageKeyThumb(domain.getStorageKeyThumb());
        entity.setStorageKeyMedium(domain.getStorageKeyMedium());
        entity.setStorageKeyOriginal(domain.getStorageKeyOriginal());
        entity.setContentType(domain.getContentType());
        entity.setSizeBytes(domain.getSizeBytes());
        entity.setWidth(domain.getWidth());
        entity.setHeight(domain.getHeight());
        entity.setSortOrder(domain.getSortOrder());
        entity.setPrimary(domain.isPrimary());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
    }

    public List<MediaAsset> toDomainList(List<MediaAssetJpaEntity> entities) {
        List<MediaAsset> result = new ArrayList<>(entities.size());
        for (MediaAssetJpaEntity entity : entities) {
            result.add(toDomain(entity));
        }
        return result;
    }
}