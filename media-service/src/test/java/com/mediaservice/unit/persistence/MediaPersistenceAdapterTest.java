package com.mediaservice.unit.persistence;

import com.mediaservice.domain.model.MediaAsset;
import com.mediaservice.infrastructure.persistence.adapter.MediaPersistenceAdapter;
import com.mediaservice.infrastructure.persistence.entity.MediaAssetJpaEntity;
import com.mediaservice.infrastructure.persistence.mapper.MediaEntityMapper;
import com.mediaservice.infrastructure.persistence.repository.MediaAssetRepository;
import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT - repository ve mapper mock'lanir, adapter mantigi izole test edilir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - MediaPersistenceAdapter")
class MediaPersistenceAdapterTest {

    @Mock private MediaAssetRepository repository;

    private final MediaEntityMapper mapper = new MediaEntityMapper();
    private MediaPersistenceAdapter adapter;

    private UUID productId;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        adapter = new MediaPersistenceAdapter(repository, mapper);
        productId = UUID.randomUUID();
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("U1: save - Yeni kayit icin (findById bos) version 0'a set edilir")
    void save_WhenNewEntity_ShouldInitializeVersionToZero() {
        MediaAsset asset = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, true);
        when(repository.findById(asset.getId())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adapter.save(asset);

        ArgumentCaptor<MediaAssetJpaEntity> captor = ArgumentCaptor.forClass(MediaAssetJpaEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("U2: save - Var olan yonetilen entity'nin version'i EZILMEZ")
    void save_WhenExistingEntity_ShouldPreserveManagedVersion() {
        MediaAsset asset = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, true);
        MediaAssetJpaEntity managed = new MediaAssetJpaEntity();
        managed.setId(asset.getId());
        managed.setVersion(7L);
        when(repository.findById(asset.getId())).thenReturn(Optional.of(managed));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adapter.save(asset);

        ArgumentCaptor<MediaAssetJpaEntity> captor = ArgumentCaptor.forClass(MediaAssetJpaEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(7L);
    }

    @Test
    @DisplayName("U3: save - webp invaryanti ihlal edilirse repository'ye HIC gidilmez")
    void save_WhenInvariantViolated_ShouldNotTouchRepository() {
        MediaAsset invalid = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, true);
        invalid.setUrl("https://cdn.test/not-webp.png");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.save(invalid))
                .isInstanceOf(IllegalStateException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("U4: saveAll - Her asset icin save cagirir, sirayi korur")
    void saveAll_ShouldPreserveOrder() {
        MediaAsset a1 = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, true);
        MediaAsset a2 = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 1, false);
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<MediaAsset> result = adapter.saveAll(List.of(a1, a2));

        assertThat(result).extracting(MediaAsset::getId).containsExactly(a1.getId(), a2.getId());
    }

    @Test
    @DisplayName("U5: findActiveByProductIdsOrderBySortOrder - bos liste icin sorgusuz bos donuş")
    void findActiveByProductIdsOrderBySortOrder_WhenEmpty_ShouldNotQuery() {
        assertThat(adapter.findActiveByProductIdsOrderBySortOrder(List.of())).isEmpty();
        assertThat(adapter.findActiveByProductIdsOrderBySortOrder(null)).isEmpty();
        verify(repository, never()).findActiveByProductIds(any());
    }

    @Test
    @DisplayName("U6: findById/lockActiveByProductIdForUpdate/countActiveByProductId - repository'ye dogru delege eder")
    void readOperations_ShouldDelegateToRepository() {
        UUID assetId = UUID.randomUUID();
        MediaAssetJpaEntity entity = mapper.toEntity(
                MediaTestFixtures.activeAsset(assetId, productId, storeId, 0, true));
        when(repository.findById(assetId)).thenReturn(Optional.of(entity));
        when(repository.lockActiveByProductId(productId)).thenReturn(List.of(entity));
        when(repository.countActiveByProductId(productId)).thenReturn(3L);

        assertThat(adapter.findById(assetId)).isPresent();
        assertThat(adapter.lockActiveByProductIdForUpdate(productId)).hasSize(1);
        assertThat(adapter.countActiveByProductId(productId)).isEqualTo(3L);
    }
}
