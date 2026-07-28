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
 * Katman: SYSTEM - siralama ve kapak (primary) gorsel degistirme akislari, yalnizca
 * public HTTP API uzerinden gercek altyapiya karsi dogrulanir.
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b>
 */
@EnabledIf("com.mediaservice.support.DockerAvailability#isDockerAvailable")
@DisplayName("SYSTEM - Siralama ve kapak gorseli degistirme (yalnizca public HTTP API)")
class MediaReorderAndPrimarySystemTest extends AbstractMediaSystemTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final ParameterizedTypeReference<List<Map<String, Object>>> MEDIA_LIST =
            new ParameterizedTypeReference<>() {
            };

    private String uploadAndReturnAssetId(UUID productId, UUID storeId) {
        HttpHeaders headers = authHeaders(storeId, "STORE");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", MediaTestFixtures.filePart(MediaTestFixtures.validPngBytes(), "image/png", "image"));

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/media/products/{productId}/images", HttpMethod.POST,
                new HttpEntity<>(body, headers), new ParameterizedTypeReference<>() {
                }, productId);
        return (String) response.getBody().get("assetId");
    }

    @Test
    @DisplayName("SYS1: Siralama sonrasi public GET dogru sirada (sortOrder ASC) doner")
    void reorder_ShouldBeReflectedInSubsequentPublicRead() {
        UUID productId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        String first = uploadAndReturnAssetId(productId, storeId);
        String second = uploadAndReturnAssetId(productId, storeId);

        HttpHeaders headers = authHeaders(storeId, "STORE");
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"assetIds\":[\"%s\",\"%s\"]}", second, first);

        ResponseEntity<String> reorder = restTemplate.exchange(
                "/api/v1/media/products/{productId}/images/order", HttpMethod.PUT,
                new HttpEntity<>(body, headers), String.class, productId);
        assertThat(reorder.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<List<Map<String, Object>>> read = restTemplate.exchange(
                "/api/v1/media/products/{productId}/images", HttpMethod.GET,
                HttpEntity.EMPTY, MEDIA_LIST, productId);

        assertThat(read.getBody()).extracting(m -> m.get("assetId"))
                .containsExactly(second, first);
    }

    @Test
    @DisplayName("SYS2: Kapak gorseli degistirilince eski primary API'de false, yenisi true doner")
    void setPrimary_ShouldBeReflectedInSubsequentPublicRead() {
        UUID productId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        String first = uploadAndReturnAssetId(productId, storeId);
        String second = uploadAndReturnAssetId(productId, storeId);

        ResponseEntity<Map<String, Object>> setPrimary = restTemplate.exchange(
                "/api/v1/media/images/{assetId}/primary", HttpMethod.PUT,
                new HttpEntity<>(authHeaders(storeId, "STORE")), new ParameterizedTypeReference<>() {
                }, second);
        assertThat(setPrimary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(setPrimary.getBody()).containsEntry("primary", true);

        ResponseEntity<List<Map<String, Object>>> read = restTemplate.exchange(
                "/api/v1/media/products/{productId}/images", HttpMethod.GET,
                HttpEntity.EMPTY, MEDIA_LIST, productId);

        Map<String, Object> firstAfter = read.getBody().stream()
                .filter(m -> m.get("assetId").equals(first)).findFirst().orElseThrow();
        Map<String, Object> secondAfter = read.getBody().stream()
                .filter(m -> m.get("assetId").equals(second)).findFirst().orElseThrow();
        assertThat(firstAfter).containsEntry("primary", false);
        assertThat(secondAfter).containsEntry("primary", true);
    }

    @Test
    @DisplayName("SYS3: Eksik/yanlis assetId listesiyle siralama 400 ile reddedilir, mevcut sira DEGISMEZ")
    void reorder_WhenAssetIdsMismatchProductImages_ShouldReturn400() {
        UUID productId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        uploadAndReturnAssetId(productId, storeId);

        HttpHeaders headers = authHeaders(storeId, "STORE");
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"assetIds\":[\"%s\"]}", UUID.randomUUID());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/media/products/{productId}/images/order", HttpMethod.PUT,
                new HttpEntity<>(body, headers), String.class, productId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
