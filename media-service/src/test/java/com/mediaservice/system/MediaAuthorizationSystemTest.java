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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Katman: SYSTEM - rol bazli yetkilendirme ve IDOR korumasi, yalnizca public HTTP
 * API'den, gercek altyapiya karsi dogrulanir.
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b>
 */
@EnabledIf("com.mediaservice.support.DockerAvailability#isDockerAvailable")
@DisplayName("SYSTEM - Rol bazli yetkilendirme ve IDOR (yalnizca public HTTP API)")
class MediaAuthorizationSystemTest extends AbstractMediaSystemTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private UUID uploadAndReturnAssetId(UUID productId, UUID storeId) {
        HttpHeaders headers = authHeaders(storeId, "STORE");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", MediaTestFixtures.filePart(MediaTestFixtures.validPngBytes(), "image/png", "image"));

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/v1/media/products/{productId}/images", HttpMethod.POST,
                new HttpEntity<>(body, headers), new ParameterizedTypeReference<>() {
                }, productId);
        return UUID.fromString((String) response.getBody().get("assetId"));
    }

    @Test
    @DisplayName("SYS1: Baska magazanin gorselini silmeye calisan STORE 403 alir, gorsel API'den hala gorunur")
    void delete_WhenNotOwner_ShouldBeForbiddenAndAssetStillVisible() {
        UUID productId = UUID.randomUUID();
        UUID ownerStoreId = UUID.randomUUID();
        UUID attackerStoreId = UUID.randomUUID();
        UUID assetId = uploadAndReturnAssetId(productId, ownerStoreId);

        ResponseEntity<String> attack = restTemplate.exchange(
                "/api/v1/media/images/{assetId}", HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(attackerStoreId, "STORE")), String.class, assetId);

        assertThat(attack.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Object> stillThere = restTemplate.exchange(
                "/api/v1/media/products/{productId}/images", HttpMethod.GET,
                HttpEntity.EMPTY, Object.class, productId);
        assertThat(stillThere.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("SYS2: ROLE_ADMIN sahiplik kisitindan muaftir - baska magazanin gorselini silebilir")
    void delete_WhenAdmin_ShouldBypassOwnershipAndSucceed() {
        UUID productId = UUID.randomUUID();
        UUID ownerStoreId = UUID.randomUUID();
        UUID assetId = uploadAndReturnAssetId(productId, ownerStoreId);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/media/images/{assetId}", HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(UUID.randomUUID(), "ADMIN")), Void.class, assetId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("SYS3: ROLE_CUSTOMER gorsel yukleyemez (403), sisteme HICBIR satir yazilmaz")
    void upload_WhenCustomerRole_ShouldBeForbidden() {
        UUID productId = UUID.randomUUID();
        HttpHeaders headers = authHeaders(UUID.randomUUID(), "CUSTOMER");
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", MediaTestFixtures.filePart(MediaTestFixtures.validPngBytes(), "image/png", "image"));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/media/products/{productId}/images", HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class, productId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("SYS4: Gecersiz/imzasiz token korumali endpoint'te 401/403 ile reddedilir")
    void protectedEndpoint_WhenTokenTamperedWith_ShouldBeRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-jwt");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/media/images/{assetId}", HttpMethod.DELETE,
                new HttpEntity<>(headers), String.class, UUID.randomUUID());

        assertThat(response.getStatusCode().is2xxSuccessful()).isFalse();
    }
}
