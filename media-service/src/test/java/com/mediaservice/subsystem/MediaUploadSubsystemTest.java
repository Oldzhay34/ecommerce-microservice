package com.mediaservice.subsystem;

import com.mediaservice.application.port.in.MediaCommandUseCase;
import com.mediaservice.domain.exception.MediaLimitExceededException;
import com.mediaservice.domain.exception.UnsupportedMediaFormatException;
import com.mediaservice.domain.model.MediaAsset;
import com.mediaservice.infrastructure.persistence.entity.MediaAssetJpaEntity;
import com.mediaservice.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.mediaservice.infrastructure.persistence.repository.MediaAssetRepository;
import com.mediaservice.infrastructure.persistence.repository.OutboxEventRepository;
import com.mediaservice.support.MediaTestFixtures;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Katman: SUBSYSTEM - upload akisi gercek Postgres + gercek MinIO + gercek Scrimage
 * WebP donusumu uzerinden ucdan uca dogrulanir.
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b>
 */
@EnabledIf("com.mediaservice.support.DockerAvailability#isDockerAvailable")
@DisplayName("SUBSYSTEM - Gorsel yukleme (Postgres + MinIO + gercek WebP donusumu)")
class MediaUploadSubsystemTest extends AbstractMediaSubsystemTest {

    @Autowired private MediaCommandUseCase commandUseCase;
    @Autowired private MediaAssetRepository mediaAssetRepository;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private MinioClient minioClient;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    @Value("${minio.bucket}")
    private String bucket;

    private UUID productId;
    private UUID storeId;

    @BeforeEach
    void resetState() {
        resetDatabase();
        productId = UUID.randomUUID();
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("S1: uploadProductImage - Gercek PNG WebP'ye donusturulur, MinIO'ya yazilir, Postgres'e ilk gorsel olarak (primary) kaydedilir")
    void uploadProductImage_ShouldConvertAndPersistAsFirstPrimaryImage() throws Exception {
        MediaAsset saved = commandUseCase.uploadProductImage(
                productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());

        assertThat(saved.isPrimary()).isTrue();
        assertThat(saved.getSortOrder()).isZero();
        assertThat(saved.getContentType()).isEqualTo("image/webp");

        List<MediaAssetJpaEntity> rows = mediaAssetRepository.findAll();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getProductId()).isEqualTo(productId);
        assertThat(rows.get(0).getStoreId()).isEqualTo(storeId);

        // Nesnenin gercekten MinIO'da var oldugunu dogrula (sadece URL degil, gercek yazim).
        minioClient.statObject(StatObjectArgs.builder()
                .bucket(bucket)
                .object(saved.getStorageKey())
                .build());
    }

    @Test
    @DisplayName("S2: uploadProductImage - Ikinci gorsel primary OLMAZ ve sortOrder artar")
    void uploadProductImage_WhenSecondImage_ShouldNotBePrimary() {
        commandUseCase.uploadProductImage(productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());

        MediaAsset second = commandUseCase.uploadProductImage(
                productId, storeId, false, "image/jpeg", MediaTestFixtures.validJpegBytes());

        assertThat(second.isPrimary()).isFalse();
        assertThat(second.getSortOrder()).isEqualTo(1);
        assertThat(mediaAssetRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("S3: uploadProductImage - Ayni transaction'da outbox_event satiri (MediaUploadedEvent) yazilir")
    void uploadProductImage_ShouldWriteOutboxEventInSameTransaction() {
        commandUseCase.uploadProductImage(productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());

        List<OutboxEventJpaEntity> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("MediaUploadedEvent");
        assertThat(events.get(0).getRoutingKey()).isEqualTo("media.uploaded");
        assertThat(events.get(0).isProcessed()).isFalse();
        assertThat(events.get(0).getPayload()).contains(productId.toString());
    }

    @Test
    @DisplayName("S4: uploadProductImage - Basarili yukleme sonrasi ilgili urunun Redis cache anahtari invalidate edilir")
    void uploadProductImage_ShouldInvalidateRedisCacheKey() {
        redisTemplate.opsForValue().set("media:product:" + productId, List.of("stale"));

        commandUseCase.uploadProductImage(productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());

        assertThat(redisTemplate.hasKey("media:product:" + productId)).isFalse();
    }

    @Test
    @DisplayName("S5: uploadProductImage - Desteklenmeyen format icin hicbir DB satiri yazilmaz")
    void uploadProductImage_WhenUnsupportedFormat_ShouldNotPersistAnything() {
        assertThatThrownBy(() -> commandUseCase.uploadProductImage(
                productId, storeId, false, "image/gif", MediaTestFixtures.garbageBytes()))
                .isInstanceOf(UnsupportedMediaFormatException.class);

        assertThat(mediaAssetRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("S6: uploadProductImage - Limit (media.max-images-per-product) asilirsa reddedilir, kilit gercekten calisir")
    void uploadProductImage_WhenLimitReached_ShouldThrowUnderRealLock() {
        for (int i = 0; i < 10; i++) {
            commandUseCase.uploadProductImage(productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());
        }

        assertThatThrownBy(() -> commandUseCase.uploadProductImage(
                productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes()))
                .isInstanceOf(MediaLimitExceededException.class);

        assertThat(mediaAssetRepository.findAll()).hasSize(10);
    }
}
