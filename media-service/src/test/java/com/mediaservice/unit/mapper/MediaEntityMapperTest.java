package com.mediaservice.unit.mapper;

import com.mediaservice.domain.model.MediaAsset;
import com.mediaservice.domain.model.MediaStatus;
import com.mediaservice.infrastructure.persistence.entity.MediaAssetJpaEntity;
import com.mediaservice.infrastructure.persistence.mapper.MediaEntityMapper;
import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Katman: UNIT - domain <-> JpaEntity donusumu, framework/mock yok.
 */
@DisplayName("UNIT - MediaEntityMapper")
class MediaEntityMapperTest {

    private final MediaEntityMapper mapper = new MediaEntityMapper();
    private final UUID productId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();

    private MediaAsset domain;

    @BeforeEach
    void setUp() {
        domain = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 2, true);
        domain.setVersion(5L);
    }

    @Test
    @DisplayName("U1: toDomain(null) ve toEntity(null) - null guvenli, null doner")
    void nullInput_ShouldReturnNull() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("U2: toEntity - tum alanlar (version dahil) birebir tasinir")
    void toEntity_ShouldCopyAllFieldsIncludingVersion() {
        MediaAssetJpaEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getProductId()).isEqualTo(domain.getProductId());
        assertThat(entity.getStoreId()).isEqualTo(domain.getStoreId());
        assertThat(entity.getUrl()).isEqualTo(domain.getUrl());
        assertThat(entity.getThumbUrl()).isEqualTo(domain.getThumbUrl());
        assertThat(entity.getOriginalUrl()).isEqualTo(domain.getOriginalUrl());
        assertThat(entity.getStorageKey()).isEqualTo(domain.getStorageKey());
        assertThat(entity.getContentType()).isEqualTo(domain.getContentType());
        assertThat(entity.getSizeBytes()).isEqualTo(domain.getSizeBytes());
        assertThat(entity.getWidth()).isEqualTo(domain.getWidth());
        assertThat(entity.getHeight()).isEqualTo(domain.getHeight());
        assertThat(entity.getSortOrder()).isEqualTo(domain.getSortOrder());
        assertThat(entity.isPrimary()).isEqualTo(domain.isPrimary());
        assertThat(entity.getStatus()).isEqualTo(domain.getStatus());
        assertThat(entity.getCreatedAt()).isEqualTo(domain.getCreatedAt());
        assertThat(entity.getVersion()).isEqualTo(5L);
    }

    @Test
    @DisplayName("U3: toDomain - JpaEntity'den domain'e tum alanlar birebir tasinir")
    void toDomain_ShouldCopyAllFields() {
        MediaAssetJpaEntity entity = mapper.toEntity(domain);

        MediaAsset roundTripped = mapper.toDomain(entity);

        assertThat(roundTripped.getId()).isEqualTo(domain.getId());
        assertThat(roundTripped.getStatus()).isEqualTo(MediaStatus.ACTIVE);
        assertThat(roundTripped.isPrimary()).isTrue();
        assertThat(roundTripped.getSortOrder()).isEqualTo(2);
        assertThat(roundTripped.getVersion()).isEqualTo(5L);
    }

    @Test
    @DisplayName("U4: applyToEntity - managed entity uzerine yazarken version alanina DOKUNMAZ")
    void applyToEntity_ShouldNeverTouchVersionField() {
        MediaAssetJpaEntity managedEntity = new MediaAssetJpaEntity();
        managedEntity.setVersion(42L);

        mapper.applyToEntity(domain, managedEntity);

        assertThat(managedEntity.getVersion())
                .as("version JPA tarafindan yonetilir, mapper'in ELLEMEMESI gerekir")
                .isEqualTo(42L);
        assertThat(managedEntity.getUrl()).isEqualTo(domain.getUrl());
    }

    @Test
    @DisplayName("U5: toDomainList - bos liste ve dolu liste dogru sirada donusturulur")
    void toDomainList_ShouldMapInOrder() {
        MediaAssetJpaEntity e1 = mapper.toEntity(domain);
        MediaAsset domain2 = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 3, false);
        MediaAssetJpaEntity e2 = mapper.toEntity(domain2);

        List<MediaAsset> result = mapper.toDomainList(List.of(e1, e2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(domain.getId());
        assertThat(result.get(1).getId()).isEqualTo(domain2.getId());
        assertThat(mapper.toDomainList(List.of())).isEmpty();
    }
}
