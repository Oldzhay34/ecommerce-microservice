package com.gateway.integration;

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/**
 * I: CORS + JWT filtresinin birlikte calismasi.
 * Preflight istegi Authorization header'i tasimaz; 401'e takilmamalidir.
 */
class CorsIntegrationTest extends AbstractGatewayIntegrationTest {

    @Test
    @DisplayName("I1: Korumali route'a yapilan CORS preflight (OPTIONS) 401'e takilmaz, CORS header'i doner")
    void cors_WhenPreflightOnProtectedRoute_ShouldNotBeRejectedByJwtFilter() {
        webTestClient.options().uri("/api/orders/1")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    @Test
    @DisplayName("I2: Izin verilmeyen origin'den gelen preflight 403 ile reddedilir")
    void cors_WhenPreflightFromDisallowedOrigin_ShouldReturnForbidden() {
        webTestClient.options().uri("/api/orders/1")
                .header(HttpHeaders.ORIGIN, "http://kotu-site.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("I3: Izin verilen origin'den gelen gercek istek CORS header'i ile yanitlanir")
    void cors_WhenActualRequestFromAllowedOrigin_ShouldExposeAllowOriginHeader() {
        DOWNSTREAM.enqueue(new MockResponse().setResponseCode(200).setBody("[]"));

        webTestClient.get().uri("/api/v1/products/search")
                .header(HttpHeaders.ORIGIN, "http://localhost:3001")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3001");
    }
}
