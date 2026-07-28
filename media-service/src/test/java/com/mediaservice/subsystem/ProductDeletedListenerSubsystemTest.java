package com.mediaservice.subsystem;

import com.mediaservice.application.port.in.MediaCommandUseCase;
import com.mediaservice.domain.model.MediaStatus;
import com.mediaservice.infrastructure.config.RabbitMqConfig;
import com.mediaservice.infrastructure.persistence.entity.MediaAssetJpaEntity;
import com.mediaservice.infrastructure.persistence.repository.MediaAssetRepository;
import com.mediaservice.infrastructure.persistence.repository.OutboxEventRepository;
import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Katman: SUBSYSTEM - IKI SERVISLI senaryo: media-service, product-service ile
 * {@code product.exchange}/{@code product.deleted} uzerinden RabbitMQ araciligiyla
 * haberlesir (bkz. {@link com.mediaservice.infrastructure.messaging.listener.ProductDeletedEventListener}).
 * <p>
 * <b>ONEMLI SINIR:</b> product-service'in kaynak kodu incelendiginde, su an
 * {@code product.exchange}/{@code product.deleted} uzerine HICBIR SEY yayinlamadigi
 * dogrulandi (product-service kendi RabbitMQConfig'inde yalnizca {@code ecommerce.topic}/
 * {@code catalog.event.*} tanimliyor; ProductDeletedEventListener'in kendi kod yorumu da
 * bunu "dogrulanmasi gereken bir varsayim" olarak isaretliyor). Bu nedenle, product-service'in
 * SOZLESMESINI (payload semasi + exchange/routing key) GERCEK bir RabbitMQ broker'ina
 * (Testcontainers) ham bir AMQP istemcisiyle basarak simule ediyoruz - TIPKI payment
 * servisinin {@code PaymentLifecycleSubsystemTest}'te order-service'in {@code order.approved}
 * olayini simule ettigi gibi. Boylece media-service'in DINLEYICI tarafi (deserileştirme ->
 * use case cagrisi -> DB cascade -> cache invalidate) gercek broker uzerinden UCTAN UCA
 * dogrulanir; product-service'in kendi Spring context'i bu test kapsamina DAHIL DEGILDIR.
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b>
 */
@EnabledIf("com.mediaservice.support.DockerAvailability#isDockerAvailable")
@DisplayName("SUBSYSTEM (cross-service) - product.deleted olayi -> media-service kaskad silme")
class ProductDeletedListenerSubsystemTest extends AbstractMediaSubsystemTest {

    @Autowired private MediaCommandUseCase commandUseCase;
    @Autowired private MediaAssetRepository mediaAssetRepository;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    private UUID productId;
    private UUID storeId;

    @BeforeEach
    void resetState() {
        resetDatabase();
        productId = UUID.randomUUID();
        storeId = UUID.randomUUID();
    }

    /** product-service'in (yayinlarsa) yapacagi gibi ham AMQP mesaji basar - uygulamanin RabbitTemplate bean'i uzerinden DEGIL, gercek broker'a. */
    private void publishProductDeletedFromExternalService(String payload) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.PRODUCT_EXCHANGE, RabbitMqConfig.RK_PRODUCT_DELETED, payload);
    }

    @Test
    @DisplayName("S1: product.deleted broker'a dustugunde, o urunun TUM ACTIVE gorselleri gercekten DB'de soft delete edilir")
    void productDeletedEvent_WhenArrivesOnBroker_ShouldCascadeSoftDeleteInDatabase() {
        commandUseCase.uploadProductImage(productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());
        commandUseCase.uploadProductImage(productId, storeId, false, "image/jpeg", MediaTestFixtures.validJpegBytes());
        assertThat(mediaAssetRepository.findAll()).hasSize(2);

        publishProductDeletedFromExternalService(MediaTestFixtures.productDeletedPayload(productId));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            List<MediaAssetJpaEntity> rows = mediaAssetRepository.findAll();
            assertThat(rows).hasSize(2);
            assertThat(rows).allMatch(e -> e.getStatus() == MediaStatus.DELETED);
        });
    }

    @Test
    @DisplayName("S2: product.deleted islendiginde ilgili urunun Redis cache anahtari invalidate edilir")
    void productDeletedEvent_ShouldInvalidateRedisCache() {
        commandUseCase.uploadProductImage(productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());
        redisTemplate.opsForValue().set("media:product:" + productId, List.of("stale"));

        publishProductDeletedFromExternalService(MediaTestFixtures.productDeletedPayload(productId));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(redisTemplate.hasKey("media:product:" + productId)).isFalse());
    }

    @Test
    @DisplayName("S3: productId eksik/gecersiz payload broker'da DUSER (islenmis sayilir), gorseller ETKILENMEZ")
    void productDeletedEvent_WhenPayloadMalformed_ShouldBeDroppedWithoutAffectingAssets() {
        var asset = commandUseCase.uploadProductImage(
                productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());

        publishProductDeletedFromExternalService(MediaTestFixtures.productDeletedPayloadInvalidProductId());

        // Kisa bir sure bekleyip gorselin ETKILENMEDIGINI dogrula (negatif kanit icin
        // 'during' kullanilir: hicbir zaman DELETED durumuna gecmemeli).
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(mediaAssetRepository.findById(asset.getId()).orElseThrow().getStatus())
                        .isEqualTo(MediaStatus.ACTIVE));
    }

    @Test
    @DisplayName("S4: product.deleted iki kez teslim edilirse (at-least-once) ikinci teslimat hata VERMEZ (idempotent)")
    void productDeletedEvent_WhenDeliveredTwice_ShouldBeIdempotent() {
        commandUseCase.uploadProductImage(productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());
        String payload = MediaTestFixtures.productDeletedPayload(productId);

        publishProductDeletedFromExternalService(payload);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(mediaAssetRepository.findAll()).allMatch(e -> e.getStatus() == MediaStatus.DELETED));

        publishProductDeletedFromExternalService(payload);

        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(mediaAssetRepository.findAll())
                        .as("ikinci teslimat softDeleteAllByProduct'i yeniden calistirsa da sonuc AYNI kalmali")
                        .allMatch(e -> e.getStatus() == MediaStatus.DELETED));
    }
}
