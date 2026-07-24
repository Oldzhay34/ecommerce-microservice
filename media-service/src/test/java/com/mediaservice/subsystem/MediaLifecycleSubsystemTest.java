package com.mediaservice.subsystem;

import com.mediaservice.application.port.in.MediaCommandUseCase;
import com.mediaservice.domain.exception.UnauthorizedMediaAccessException;
import com.mediaservice.domain.model.MediaAsset;
import com.mediaservice.infrastructure.persistence.entity.MediaAssetJpaEntity;
import com.mediaservice.infrastructure.persistence.repository.MediaAssetRepository;
import com.mediaservice.infrastructure.persistence.repository.OutboxEventRepository;
import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Katman: SUBSYSTEM - silme / kapak (primary) ayarlama / siralama akislari gercek
 * Postgres uzerinde, urun bazli PESSIMISTIC_WRITE kilit gercekten devrede iken
 * dogrulanir.
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b>
 */
@EnabledIf("com.mediaservice.support.DockerAvailability#isDockerAvailable")
@DisplayName("SUBSYSTEM - Gorsel yasam dongusu (Postgres, gercek pessimistic lock)")
class MediaLifecycleSubsystemTest extends AbstractMediaSubsystemTest {

    @Autowired private MediaCommandUseCase commandUseCase;
    @Autowired private MediaAssetRepository mediaAssetRepository;
    @Autowired private OutboxEventRepository outboxEventRepository;

    private UUID productId;
    private UUID storeId;

    @BeforeEach
    void resetState() {
        outboxEventRepository.deleteAll();
        mediaAssetRepository.deleteAll();
        productId = UUID.randomUUID();
        storeId = UUID.randomUUID();
    }

    private MediaAsset upload(String contentType, byte[] bytes) {
        return commandUseCase.uploadProductImage(productId, storeId, false, contentType, bytes);
    }

    @Test
    @DisplayName("S1: deleteProductImage - Primary silinince kalan gorsellerden en dusuk sortOrder'li DB'de gercekten primary olur")
    void deleteProductImage_WhenPrimaryDeleted_ShouldPersistHandoffToLowestSortOrder() {
        MediaAsset first = upload("image/png", MediaTestFixtures.validPngBytes());
        MediaAsset second = upload("image/jpeg", MediaTestFixtures.validJpegBytes());

        commandUseCase.deleteProductImage(first.getId(), storeId, false);

        List<MediaAssetJpaEntity> remaining = mediaAssetRepository.findAll().stream()
                .filter(e -> e.getStatus() == com.mediaservice.domain.model.MediaStatus.ACTIVE)
                .toList();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getId()).isEqualTo(second.getId());
        assertThat(remaining.get(0).isPrimary()).isTrue();
    }

    @Test
    @DisplayName("S2: deleteProductImage - Silinen satir DB'de fiziksel olarak KALIR (soft delete), status=DELETED")
    void deleteProductImage_ShouldSoftDeleteNotHardDelete() {
        MediaAsset asset = upload("image/png", MediaTestFixtures.validPngBytes());

        commandUseCase.deleteProductImage(asset.getId(), storeId, false);

        MediaAssetJpaEntity row = mediaAssetRepository.findById(asset.getId()).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(com.mediaservice.domain.model.MediaStatus.DELETED);
        assertThat(row.isPrimary()).isFalse();
    }

    @Test
    @DisplayName("S3: deleteProductImage - Baska magazanin gorseli DB seviyesinde de silinemez (IDOR)")
    void deleteProductImage_WhenNotOwner_ShouldNotModifyRow() {
        MediaAsset asset = upload("image/png", MediaTestFixtures.validPngBytes());
        UUID attacker = UUID.randomUUID();

        assertThatThrownBy(() -> commandUseCase.deleteProductImage(asset.getId(), attacker, false))
                .isInstanceOf(UnauthorizedMediaAccessException.class);

        assertThat(mediaAssetRepository.findById(asset.getId()).orElseThrow().getStatus())
                .isEqualTo(com.mediaservice.domain.model.MediaStatus.ACTIVE);
    }

    @Test
    @DisplayName("S4: setPrimaryImage - uq_media_asset_primary kismi indeksi ihlal edilmeden primary devri DB'ye yazilir")
    void setPrimaryImage_ShouldPersistPrimarySwitchWithoutUniqueConstraintViolation() {
        MediaAsset first = upload("image/png", MediaTestFixtures.validPngBytes());
        MediaAsset second = upload("image/jpeg", MediaTestFixtures.validJpegBytes());
        assertThat(first.isPrimary()).isTrue();

        commandUseCase.setPrimaryImage(second.getId(), storeId, false);

        assertThat(mediaAssetRepository.findById(first.getId()).orElseThrow().isPrimary()).isFalse();
        assertThat(mediaAssetRepository.findById(second.getId()).orElseThrow().isPrimary()).isTrue();
        assertThat(mediaAssetRepository.findAll().stream().filter(MediaAssetJpaEntity::isPrimary))
                .as("ayni urun icin DB'de asla ikiden fazla/az primary olamaz")
                .hasSize(1);
    }

    @Test
    @DisplayName("S5: reorderProductImages - Verilen sirayla sortOrder DB'ye kalici olarak yazilir")
    void reorderProductImages_ShouldPersistNewSortOrder() {
        MediaAsset first = upload("image/png", MediaTestFixtures.validPngBytes());
        MediaAsset second = upload("image/jpeg", MediaTestFixtures.validJpegBytes());

        commandUseCase.reorderProductImages(productId, List.of(second.getId(), first.getId()), storeId, false);

        assertThat(mediaAssetRepository.findById(second.getId()).orElseThrow().getSortOrder()).isEqualTo(0);
        assertThat(mediaAssetRepository.findById(first.getId()).orElseThrow().getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("S6: softDeleteAllByProduct - Urunun TUM ACTIVE gorselleri DB'de soft delete edilir")
    void softDeleteAllByProduct_ShouldSoftDeleteEveryActiveRow() {
        upload("image/png", MediaTestFixtures.validPngBytes());
        upload("image/jpeg", MediaTestFixtures.validJpegBytes());

        commandUseCase.softDeleteAllByProduct(productId);

        assertThat(mediaAssetRepository.findAll())
                .allMatch(e -> e.getStatus() == com.mediaservice.domain.model.MediaStatus.DELETED);
    }
}
