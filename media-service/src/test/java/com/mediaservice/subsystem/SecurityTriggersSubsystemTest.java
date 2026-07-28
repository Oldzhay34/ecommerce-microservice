package com.mediaservice.subsystem;

import com.mediaservice.application.port.in.MediaCommandUseCase;
import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Katman: SUBSYSTEM - V2__security_triggers.sql'in gercek Postgres uzerinde
 * GERCEKTEN devrede oldugunu dogrular. Uygulama katmanini KASITLI OLARAK bypass
 * edip ham JDBC ile dogrudan yasak islemler denenir - trigger'lar bu son savunma
 * hattinin GERCEKTEN calistigini kanitlar (yalnizca uygulama kodunun bu islemleri
 * yapmadigini degil).
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b>
 * <p>
 * <b>Beklenen exception tipi hakkinda:</b> trigger'lar {@code RAISE EXCEPTION} kullanir,
 * bu da SQLSTATE {@code P0001} (raise_exception) uretir. Spring'in varsayilan
 * {@code SQLStateSQLExceptionTranslator}'i "P0" sinifini bilmedigi icin bunu
 * {@code DataIntegrityViolationException}'a DEGIL, {@link DataAccessException}'in bir
 * alt tipi olan {@code UncategorizedSQLException}'a cevirir. Asil kanit zaten trigger'in
 * kendi mesajidir; bu yuzden tip iddiasi {@link DataAccessException} seviyesinde tutulur.
 */
@EnabledIf("com.mediaservice.support.DockerAvailability#isDockerAvailable")
@DisplayName("SUBSYSTEM - Guvenlik trigger'lari (Postgres, uygulama katmani bypass edilerek)")
class SecurityTriggersSubsystemTest extends AbstractMediaSubsystemTest {

    @Autowired private MediaCommandUseCase commandUseCase;

    private UUID productId;
    private UUID storeId;

    @BeforeEach
    void resetState() {
        resetDatabase();
        productId = UUID.randomUUID();
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("T1: media_asset uzerinde ham DELETE, uygulama katmani bypass edilse bile trigger tarafindan reddedilir")
    void directDelete_ShouldBeRejectedByTrigger() {
        var asset = commandUseCase.uploadProductImage(
                productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM media_asset WHERE id = ?", asset.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("fiziksel olarak silinemez");
    }

    @Test
    @DisplayName("T2: outbox_event.payload gecersiz JSON ile dogrudan INSERT edilirse trigger tarafindan reddedilir")
    void insertingInvalidJsonPayload_ShouldBeRejectedByTrigger() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO outbox_event (id, aggregate_type, aggregate_id, event_type, routing_key, payload, processed, created_at) " +
                        "VALUES (?, 'MediaAsset', ?, 'MediaUploadedEvent', 'media.uploaded', ?, false, ?)",
                UUID.randomUUID(), productId, "{this is not json", java.sql.Timestamp.from(Instant.now())))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("gecerli JSON");
    }

    @Test
    @DisplayName("T3: media_asset icin negatif size_bytes ile dogrudan INSERT reddedilir")
    void insertingNegativeSizeBytes_ShouldBeRejectedByTrigger() {
        UUID assetId = UUID.randomUUID();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO media_asset (id, product_id, store_id, url, thumb_url, original_url, " +
                        "storage_key, storage_key_thumb, storage_key_medium, storage_key_original, " +
                        "content_type, size_bytes, width, height, sort_order, is_primary, status, created_at, version) " +
                        "VALUES (?, ?, ?, 'https://cdn.test/a.webp', 'https://cdn.test/t.webp', 'https://cdn.test/o.webp', " +
                        "'k', 'kt', 'km', 'ko', 'image/webp', -1, 10, 10, 0, false, 'ACTIVE', ?, 0)",
                assetId, productId, storeId, java.sql.Timestamp.from(Instant.now())))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("pozitif olmalidir");
    }

    @Test
    @DisplayName("T4: masum akis ETKILENMEZ - normal yukleme, silme ve outbox yazimi trigger'lardan sonra da sorunsuz calisir")
    void legitimateFlow_ShouldRemainUnaffected() {
        var asset = commandUseCase.uploadProductImage(
                productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());
        assertThat(asset).isNotNull();

        commandUseCase.deleteProductImage(asset.getId(), storeId, false);

        Long processedCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_event WHERE aggregate_id = ?", Long.class, productId);
        assertThat(processedCount).isGreaterThan(0);
    }
}
