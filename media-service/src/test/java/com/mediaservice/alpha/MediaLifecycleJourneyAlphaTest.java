package com.mediaservice.alpha;

import com.mediaservice.infrastructure.config.RabbitMqConfig;
import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Katman: ALPHA - uctan uca, black-box, tek surekli yolculuk.
 * <p>
 * Akis: magaza 3 gorsel yukler (outbox'in GERCEKTEN broker'a bastigi dogrulanir) ->
 * toplu (batch) sorgu -> yeniden siralama -> kapak degistirme -> mevcut kapagi silme
 * (handoff dogrulanir) -> product-service'in product.deleted olayi bagimsiz bir AMQP
 * istemcisiyle yayinlanir -> kalan gorseller kaskad soft-delete edilir (yalnizca public
 * API'den gozlemlenir) -> bu kaskadin da kendi outbox olayini (media.deleted) broker'a
 * gercekten bastigi dogrulanir.
 * <p>
 * Hicbir uygulama bean'i inject EDILMEZ (yalnizca HTTP istemcisi + bagimsiz AMQP istemcisi).
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b>
 */
@EnabledIf("com.mediaservice.support.DockerAvailability#isDockerAvailable")
@DisplayName("ALPHA - Gorsel yasam dongusu yolculugu (yalnizca public HTTP API + bagimsiz AMQP)")
class MediaLifecycleJourneyAlphaTest extends AbstractMediaAlphaTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final ParameterizedTypeReference<List<Map<String, Object>>> MEDIA_LIST =
            new ParameterizedTypeReference<>() {
            };

    private String upload(UUID productId, UUID storeId, byte[] bytes) {
        HttpHeaders headers = authHeaders(storeId, "STORE");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "image";
            }
        });

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/media/products/{productId}/images", HttpMethod.POST,
                new HttpEntity<>(body, headers), new ParameterizedTypeReference<>() {
                }, productId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) response.getBody().get("assetId");
    }

    private List<Map<String, Object>> currentImages(UUID productId) {
        return restTemplate.exchange("/api/v1/media/products/{productId}/images", HttpMethod.GET,
                HttpEntity.EMPTY, MEDIA_LIST, productId).getBody();
    }

    @Test
    @DisplayName("A1: Yukleme -> siralama -> kapak degistirme -> silme -> urun-silindi kaskadi, uctan uca")
    void fullLifecycle_ShouldBeObservableOnlyThroughPublicApiAndBroker() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        // 1) Ilk gorsel yuklenmeden ONCE media.uploaded kuyrugu baglanir (topic exchange
        // once dinleyen olmadan basilan mesaji kaybeder).
        ListenerHandle uploadedListener = bindQueue(RabbitMqConfig.RK_MEDIA_UPLOADED);

        String first = upload(productId, storeId, MediaTestFixtures.validPngBytes());
        String second = upload(productId, storeId, MediaTestFixtures.validJpegBytes());
        String third = upload(productId, storeId, MediaTestFixtures.validWebpBytes());

        // 2) Outbox'in GERCEKTEN broker'a bastigini kanitla (yalnizca DB satirini degil).
        String uploadedMessage = awaitMessage(uploadedListener, 20_000);
        assertThat(uploadedMessage).contains(productId.toString());

        // 3) Toplu sorgu - 3 gorsel de gorunur.
        ResponseEntity<Map<String, Object>> batch = restTemplate.exchange(
                "/api/v1/media/products/images/batch", HttpMethod.POST,
                new HttpEntity<>("{\"productIds\":[\"" + productId + "\"]}", jsonHeaders()),
                new ParameterizedTypeReference<>() {
                });
        assertThat(batch.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(currentImages(productId)).hasSize(3);

        // 4) Yeniden sirala: [third, first, second]
        String reorderBody = String.format("{\"assetIds\":[\"%s\",\"%s\",\"%s\"]}", third, first, second);
        ResponseEntity<String> reorder = restTemplate.exchange(
                "/api/v1/media/products/{productId}/images/order", HttpMethod.PUT,
                new HttpEntity<>(reorderBody, authHeaders(storeId, "STORE")), String.class, productId);
        assertThat(reorder.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(currentImages(productId)).extracting(m -> m.get("assetId"))
                .containsExactly(third, first, second);

        // 5) Kapak gorselini degistir: second yeni primary olsun.
        ResponseEntity<Map<String, Object>> setPrimary = restTemplate.exchange(
                "/api/v1/media/images/{assetId}/primary", HttpMethod.PUT,
                new HttpEntity<>(authHeaders(storeId, "STORE")), new ParameterizedTypeReference<>() {
                }, second);
        assertThat(setPrimary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(setPrimary.getBody()).containsEntry("primary", true);

        // 6) Mevcut kapagi (second) sil -> devir gerceklesmeli. Kalan [third, first]
        // arasinda sortOrder'i en kucuk olan (third, 0) yeni kapak olmali.
        ResponseEntity<Void> deletePrimary = restTemplate.exchange(
                "/api/v1/media/images/{assetId}", HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(storeId, "STORE")), Void.class, second);
        assertThat(deletePrimary.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        List<Map<String, Object>> afterDelete = currentImages(productId);
        assertThat(afterDelete).hasSize(2);
        Map<String, Object> newPrimary = afterDelete.stream()
                .filter(m -> Boolean.TRUE.equals(m.get("primary"))).findFirst().orElseThrow();
        assertThat(newPrimary.get("assetId")).isEqualTo(third);

        // 7) product-service'in product.deleted olayini simule et - kaskad soft-delete
        // OLAYININ KENDI outbox olayini (media.deleted) da broker'a bastigini kanitlamak
        // icin dinleyici, olay yayinlanmadan ONCE baglanir.
        ListenerHandle deletedListener = bindQueue(RabbitMqConfig.RK_MEDIA_DELETED);
        publishProductDeletedEvent(MediaTestFixtures.productDeletedPayload(productId));

        // 8) Kaskad yalnizca public API'den gozlemlenir: urunun TUM gorselleri kaybolmali.
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(currentImages(productId)).isEmpty());

        // 9) Kaskadin kendi outbox olayi da GERCEKTEN broker'a basilmis olmali.
        String deletedMessage = awaitMessage(deletedListener, 20_000);
        assertThat(deletedMessage).contains(productId.toString());
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
