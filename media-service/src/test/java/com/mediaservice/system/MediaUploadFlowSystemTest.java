package com.mediaservice.system;

import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Katman: SYSTEM - gorsel yukleme akisi yalnizca public HTTP API uzerinden, gercek
 * altyapiya (Postgres + MinIO + RabbitMQ) karsi kara kutu olarak dogrulanir.
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b>
 */
@EnabledIf("com.mediaservice.support.DockerAvailability#isDockerAvailable")
@DisplayName("SYSTEM - Gorsel yukleme akisi (yalnizca public HTTP API)")
class MediaUploadFlowSystemTest extends AbstractMediaSystemTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final ParameterizedTypeReference<List<Map<String, Object>>> MEDIA_LIST =
            new ParameterizedTypeReference<>() {
            };

    private ResponseEntity<Map<String, Object>> uploadImage(UUID productId, UUID storeId, byte[] bytes, String contentType) {
        HttpHeaders headers = authHeaders(storeId, "STORE");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", MediaTestFixtures.filePart(bytes, contentType, "image"));

        return restTemplate.exchange("/api/v1/media/products/{productId}/images", HttpMethod.POST,
                new HttpEntity<>(body, headers), new ParameterizedTypeReference<>() {
                }, productId);
    }

    @Test
    @DisplayName("SYS1: Yukleme sonrasi 201 doner ve HEMEN akabinde public GET ile gorunur olur")
    void uploadThenPublicGet_ShouldReturnUploadedImage() {
        UUID productId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        ResponseEntity<Map<String, Object>> upload = uploadImage(productId, storeId,
                MediaTestFixtures.validPngBytes(), "image/png");

        assertThat(upload.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(upload.getBody()).containsEntry("primary", true);
        assertThat(upload.getBody().get("contentType")).isEqualTo("image/webp");

        ResponseEntity<List<Map<String, Object>>> publicRead = restTemplate.exchange(
                "/api/v1/media/products/{productId}/images", HttpMethod.GET,
                HttpEntity.EMPTY, MEDIA_LIST, productId);

        assertThat(publicRead.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicRead.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("SYS2: Gorseli olmayan urun icin public GET 200 + BOS DIZI doner, 404 DONMEZ")
    void publicGet_WhenProductHasNoImages_ShouldReturnEmptyListNot404() {
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "/api/v1/media/products/{productId}/images", HttpMethod.GET,
                HttpEntity.EMPTY, MEDIA_LIST, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("SYS3: Desteklenmeyen dosya formati icin 415 + hata govdesi (JSON semasi) doner")
    void upload_WhenUnsupportedFormat_ShouldReturn415WithErrorBody() {
        ResponseEntity<Map<String, Object>> response = uploadImage(
                UUID.randomUUID(), UUID.randomUUID(), MediaTestFixtures.garbageBytes(), "image/gif");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).containsKeys("timestamp", "status", "message", "path");
    }

    @Test
    @DisplayName("SYS4: Batch endpoint - birden fazla urunun gorselleri TEK istekle, token gerekmeden okunur")
    void batchEndpoint_ShouldReturnMultipleProductsWithoutAuth() {
        UUID productWithImage = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        uploadImage(productWithImage, storeId, MediaTestFixtures.validPngBytes(), "image/png");
        UUID productWithoutImage = UUID.randomUUID();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"productIds\":[\"%s\",\"%s\"]}", productWithImage, productWithoutImage);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/media/products/images/batch", HttpMethod.POST,
                new HttpEntity<>(body, headers), new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
