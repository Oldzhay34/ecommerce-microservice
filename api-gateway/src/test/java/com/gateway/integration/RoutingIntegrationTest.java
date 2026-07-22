package com.gateway.integration;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * I: Route eslesmesi ve proxy davranisi (path, query string, method, govde, downstream status).
 */
class RoutingIntegrationTest extends AbstractGatewayIntegrationTest {

    @Test
    @DisplayName("I1: Path predicate'i eslesen istek dogru downstream'e ayni path ile iletilir")
    void routing_WhenPathMatchesRoute_ShouldForwardSamePathToDownstream() throws Exception {
        DOWNSTREAM.enqueue(new MockResponse().setResponseCode(200).setBody("[]"));

        webTestClient.get().uri("/api/reviews/product/7")
                .exchange()
                .expectStatus().isOk();

        RecordedRequest recorded = DOWNSTREAM.takeRequest(3, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).isEqualTo("/api/reviews/product/7");
        assertThat(recorded.getMethod()).isEqualTo("GET");
    }

    @Test
    @DisplayName("I2: Query string downstream'e oldugu gibi tasinir")
    void routing_WhenRequestHasQueryString_ShouldPreserveItDownstream() throws Exception {
        DOWNSTREAM.enqueue(new MockResponse().setResponseCode(200).setBody("[]"));

        webTestClient.get().uri("/api/v1/products/search?q=telefon&page=2")
                .exchange()
                .expectStatus().isOk();

        RecordedRequest recorded = DOWNSTREAM.takeRequest(3, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).contains("q=telefon").contains("page=2");
    }

    @Test
    @DisplayName("I3: POST govdesi ve Content-Type downstream'e bozulmadan iletilir")
    void routing_WhenPostWithBody_ShouldForwardBodyAndContentType() throws Exception {
        DOWNSTREAM.enqueue(new MockResponse().setResponseCode(201).setBody("{\"id\":\"9\"}"));

        webTestClient.post().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer("user-1", "ROLE_USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"productId\":\"p-1\",\"quantity\":3}")
                .exchange()
                .expectStatus().isCreated();

        RecordedRequest recorded = DOWNSTREAM.takeRequest(3, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getHeader(HttpHeaders.CONTENT_TYPE)).contains("application/json");
        assertThat(recorded.getBody().readUtf8()).contains("p-1").contains("3");
    }

    @Test
    @DisplayName("I4: Downstream'in dondugu status ve govde client'a aynen aktarilir (4xx dahil)")
    void routing_WhenDownstreamReturnsClientError_ShouldPassStatusAndBodyThrough() {
        DOWNSTREAM.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"message\":\"Siparis bulunamadi\"}"));

        webTestClient.get().uri("/api/orders/9999")
                .header(HttpHeaders.AUTHORIZATION, bearer("user-1", "ROLE_USER"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Siparis bulunamadi");
    }

    @Test
    @DisplayName("I5: Downstream 500 dondugunde gateway status'u degistirmez, seffaf gecirir")
    void routing_WhenDownstreamReturnsServerError_ShouldNotRewriteStatus() {
        DOWNSTREAM.enqueue(new MockResponse().setResponseCode(500).setBody("downstream patladi"));

        webTestClient.get().uri("/api/orders/1")
                .header(HttpHeaders.AUTHORIZATION, bearer("user-1", "ROLE_USER"))
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("I6: Downstream'in yanit header'lari client'a iletilir")
    void routing_WhenDownstreamSetsResponseHeader_ShouldForwardItToClient() {
        DOWNSTREAM.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("X-Total-Count", "42")
                .setBody("[]"));

        webTestClient.get().uri("/api/v1/products/search")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Total-Count", "42");
    }

    @Test
    @DisplayName("I7: Hicbir route ile eslesmeyen path 404 doner ve downstream'e gitmez")
    void routing_WhenNoRouteMatches_ShouldReturnNotFoundWithoutCallingDownstream() throws Exception {
        webTestClient.get().uri("/bilinmeyen/yol")
                .exchange()
                .expectStatus().isNotFound();

        assertThat(DOWNSTREAM.takeRequest(300, TimeUnit.MILLISECONDS)).isNull();
    }
}
