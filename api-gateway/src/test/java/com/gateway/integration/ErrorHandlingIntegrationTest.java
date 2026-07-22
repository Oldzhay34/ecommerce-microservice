package com.gateway.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * I: GlobalErrorWebExceptionHandler'in gercek sunucu uzerindeki davranisi.
 * Hata govdesinin ErrorResponse formatinda oldugunu ve ic detay sizdirmadigini dogrular.
 */
class ErrorHandlingIntegrationTest extends AbstractGatewayIntegrationTest {

    @Test
    @DisplayName("I1: Erisilemeyen downstream 503 ve ErrorResponse govdesi uretir (Boot varsayilan formati DEGIL)")
    void errorHandler_WhenDownstreamIsUnreachable_ShouldReturn503WithErrorResponseBody() {
        webTestClient.get().uri("/api/dead/anything")
                .header(HttpHeaders.AUTHORIZATION, bearer("user-1", "ROLE_USER"))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.path").isEqualTo("/api/dead/anything")
                .jsonPath("$.message").isEqualTo("Hedef servise su anda ulasilamiyor. Lutfen daha sonra tekrar deneyin.")
                .jsonPath("$.timestamp").exists();
    }

    @Test
    @DisplayName("I2: Eslesmeyen route 404 doner ve govde ErrorResponse alanlarini icerir")
    void errorHandler_WhenRouteNotFound_ShouldReturnErrorResponseShaped404() {
        webTestClient.get().uri("/hic-olmayan-yol")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.path").isEqualTo("/hic-olmayan-yol")
                .jsonPath("$.message").exists()
                .jsonPath("$.timestamp").exists();
    }

    @Test
    @DisplayName("I3: 503 govdesi dahili istisna/hedef adres bilgisi sizdirmaz")
    void errorHandler_WhenDownstreamFails_ShouldNotLeakInternalDetails() {
        byte[] raw = webTestClient.get().uri("/api/dead/anything")
                .header(HttpHeaders.AUTHORIZATION, bearer("user-1", "ROLE_USER"))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody().returnResult().getResponseBodyContent();

        String body = raw == null ? "" : new String(raw);
        assertThat(body)
                .doesNotContain("ConnectException")
                .doesNotContain("java.net")
                .doesNotContain("localhost:1")
                .doesNotContain("Connection refused")
                .doesNotContain("trace");
    }

    @Test
    @DisplayName("I4: 401 govdesi de ayni ErrorResponse sozlesmesine uyar")
    void errorHandler_WhenUnauthorized_ShouldUseSameErrorResponseContract() {
        webTestClient.get().uri("/api/orders/1")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.path").isEqualTo("/api/orders/1")
                .jsonPath("$.message").exists()
                .jsonPath("$.timestamp").exists();
    }
}
