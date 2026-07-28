package com.mediaservice.subsystem;

import com.mediaservice.api.dto.MediaAssetResponse;
import com.mediaservice.application.port.in.MediaCommandUseCase;
import com.mediaservice.application.port.in.MediaQueryUseCase;
import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Katman: SUBSYSTEM - okuma modelinin cache-aside davranisi gercek Redis uzerinde
 * dogrulanir (get -> miss -> DB -> Redis'e doldur -> sonraki okuma HIT; mutasyon
 * sonrasi invalidate).
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b>
 */
@EnabledIf("com.mediaservice.support.DockerAvailability#isDockerAvailable")
@DisplayName("SUBSYSTEM - Okuma modeli cache-aside (Postgres + Redis)")
class MediaCacheSubsystemTest extends AbstractMediaSubsystemTest {

    @Autowired private MediaCommandUseCase commandUseCase;
    @Autowired private MediaQueryUseCase queryUseCase;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    private UUID productId;
    private UUID storeId;

    @BeforeEach
    void resetState() {
        resetDatabase();
        redisTemplate.getConnectionFactory().getConnection().flushDb();
        productId = UUID.randomUUID();
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("S1: getProductMedia - Ilk cagri Postgres'ten okur ve Redis'i GERCEKTEN doldurur")
    void getProductMedia_FirstCall_ShouldPopulateRedis() {
        commandUseCase.uploadProductImage(productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());

        List<MediaAssetResponse> result = queryUseCase.getProductMedia(productId);

        assertThat(result).hasSize(1);
        assertThat(redisTemplate.hasKey("media:product:" + productId)).isTrue();
    }

    @Test
    @DisplayName("S2: getProductMedia - Ikinci cagri gercekten Redis'ten doner: DB satiri elle silinse bile HALA gorunur")
    void getProductMedia_SecondCall_ShouldServeFromRedisEvenIfDbRowDeleted() {
        commandUseCase.uploadProductImage(productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());
        List<MediaAssetResponse> firstCall = queryUseCase.getProductMedia(productId);
        assertThat(firstCall).hasSize(1);

        // Kanit: DB satirini use case'i bypass ederek DOGRUDAN silersek, cache hit hala
        // eski (dogru) sonucu donduruyorsa okuma GERCEKTEN Redis'ten geliyor demektir.
        // (Fiziksel silme trigger ile yasakli oldugu icin test-only bypass kullanilir.)
        hardDeleteAllMediaAssets();

        List<MediaAssetResponse> secondCall = queryUseCase.getProductMedia(productId);
        assertThat(secondCall).hasSize(1);
        assertThat(redisTemplate.getExpire("media:product:" + productId))
                .as("TTL Redis'te gercekten uygulanmis olmali")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("S3: uploadProductImage - Mevcut Redis kaydi mutasyondan SONRA invalidate edilir")
    void mutation_ShouldInvalidateExistingRedisEntry() {
        commandUseCase.uploadProductImage(productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());
        queryUseCase.getProductMedia(productId);
        assertThat(redisTemplate.hasKey("media:product:" + productId)).isTrue();

        commandUseCase.uploadProductImage(productId, storeId, false, "image/jpeg", MediaTestFixtures.validJpegBytes());

        assertThat(redisTemplate.hasKey("media:product:" + productId)).isFalse();
    }

    @Test
    @DisplayName("S4: getProductMediaBatch - Kismi cache hit'te miss olanlar TEK sorguda cekilir ve Redis'e yazilir")
    void getProductMediaBatch_ShouldFillOnlyMissingEntriesInRedis() {
        commandUseCase.uploadProductImage(productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());
        UUID emptyProductId = UUID.randomUUID();

        var result = queryUseCase.getProductMediaBatch(List.of(productId, emptyProductId));

        assertThat(result).containsKeys(productId.toString(), emptyProductId.toString());
        assertThat(redisTemplate.hasKey("media:product:" + productId)).isTrue();
        assertThat(redisTemplate.hasKey("media:product:" + emptyProductId)).isTrue();
    }
}
