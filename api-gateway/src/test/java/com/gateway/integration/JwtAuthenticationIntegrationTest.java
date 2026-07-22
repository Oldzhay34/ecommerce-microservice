package com.gateway.integration;

import com.gateway.support.JwtTestTokens;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * I: JWT filtresinin gercek HTTP akisindaki davranisi.
 * Gateway ayakta, downstream MockWebServer; hicbir bean mock'lanmaz.
 */
class JwtAuthenticationIntegrationTest extends AbstractGatewayIntegrationTest {

    @Test
    @DisplayName("I1: GET /api/orders - Token olmadan gelen istek 401 doner ve downstream'e HIC ulasmaz")
    void protectedRoute_WhenNoToken_ShouldReturn401AndNotReachDownstream() throws Exception {
        webTestClient.get().uri("/api/orders/1")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.path").isEqualTo("/api/orders/1")
                .jsonPath("$.message").exists()
                .jsonPath("$.timestamp").exists();

        assertThat(DOWNSTREAM.takeRequest(300, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    @DisplayName("I2: GET /api/orders - Gecerli token ile istek downstream'e proxy'lenir ve 200 doner")
    void protectedRoute_WhenTokenIsValid_ShouldProxyToDownstream() throws Exception {
        DOWNSTREAM.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"1\",\"status\":\"APPROVED\"}"));

        webTestClient.get().uri("/api/orders/1")
                .header(HttpHeaders.AUTHORIZATION, bearer("user-77", "ROLE_USER"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("APPROVED");

        RecordedRequest recorded = DOWNSTREAM.takeRequest(3, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).isEqualTo("/api/orders/1");
    }

    @Test
    @DisplayName("I3: Downstream'e X-User-Id ve X-User-Role eklenir, Authorization aynen iletilir")
    void protectedRoute_WhenTokenIsValid_ShouldForwardIdentityHeadersDownstream() throws Exception {
        DOWNSTREAM.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
        String token = JwtTestTokens.valid("user-42", "ROLE_STORE");

        webTestClient.get().uri("/api/orders/mine")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        RecordedRequest recorded = DOWNSTREAM.takeRequest(3, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getHeader("X-User-Id")).isEqualTo("user-42");
        assertThat(recorded.getHeader("X-User-Role")).isEqualTo("ROLE_STORE");
        assertThat(recorded.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + token);
    }

    @Test
    @DisplayName("I4: Client'in gonderdigi sahte X-User-Id header'i downstream'e SIZMAZ, token degeriyle ezilir")
    void protectedRoute_WhenClientSpoofsIdentityHeader_ShouldOverwriteWithTokenValue() throws Exception {
        DOWNSTREAM.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        webTestClient.get().uri("/api/orders/mine")
                .header(HttpHeaders.AUTHORIZATION, bearer("user-5", "ROLE_USER"))
                .header("X-User-Id", "1")
                .header("X-User-Role", "ROLE_ADMIN")
                .exchange()
                .expectStatus().isOk();

        RecordedRequest recorded = DOWNSTREAM.takeRequest(3, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getHeaders().values("X-User-Id")).containsExactly("user-5");
        assertThat(recorded.getHeaders().values("X-User-Role")).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("I5: Suresi dolmus token 401 doner ve downstream cagrilmaz")
    void protectedRoute_WhenTokenIsExpired_ShouldReturn401() throws Exception {
        webTestClient.get().uri("/api/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestTokens.expired("user-1"))
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(DOWNSTREAM.takeRequest(300, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    @DisplayName("I6: Baska secret ile imzalanmis (sahte) token 401 doner - imza dogrulamasi uctan uca calisiyor")
    void protectedRoute_WhenTokenSignedWithForeignSecret_ShouldReturn401() throws Exception {
        webTestClient.get().uri("/api/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestTokens.signedWithWrongKey("hacker"))
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(DOWNSTREAM.takeRequest(300, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    @DisplayName("I7: Bozuk formatli token ve 'Bearer' oneksiz header 401 doner")
    void protectedRoute_WhenAuthorizationHeaderIsInvalid_ShouldReturn401() {
        webTestClient.get().uri("/api/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestTokens.malformed())
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.get().uri("/api/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("I8: Public route (/api/v1/auth/**) token olmadan downstream'e ulasir")
    void publicAuthRoute_WhenNoToken_ShouldReachDownstream() throws Exception {
        DOWNSTREAM.enqueue(new MockResponse().setResponseCode(200).setBody("{\"token\":\"x\"}"));

        webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"a@b.c\",\"password\":\"p\"}")
                .exchange()
                .expectStatus().isOk();

        RecordedRequest recorded = DOWNSTREAM.takeRequest(3, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getPath()).isEqualTo("/api/v1/auth/login");
        assertThat(recorded.getBody().readUtf8()).contains("a@b.c");
    }

    @Test
    @DisplayName("I9: Public route'ta client'in enjekte ettigi X-User-Id header'i downstream'e GECMEZ")
    void publicRoute_WhenClientInjectsIdentityHeader_ShouldStripItBeforeDownstream() throws Exception {
        DOWNSTREAM.enqueue(new MockResponse().setResponseCode(200).setBody("[]"));

        webTestClient.get().uri("/api/v1/products/search?q=telefon")
                .header("X-User-Id", "1")
                .header("X-User-Role", "ROLE_ADMIN")
                .exchange()
                .expectStatus().isOk();

        RecordedRequest recorded = DOWNSTREAM.takeRequest(3, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getHeader("X-User-Id")).isNull();
        assertThat(recorded.getHeader("X-User-Role")).isNull();
    }

    @Test
    @DisplayName("I10: GET /api/v1/media/products/{id}/images public, ayni path'e POST token ister")
    void mediaRoute_WhenGetIsPublicButPostIsProtected_ShouldEnforcePerMethodWhitelist() throws Exception {
        DOWNSTREAM.enqueue(new MockResponse().setResponseCode(200).setBody("[]"));

        webTestClient.get().uri("/api/v1/media/products/42/images")
                .exchange()
                .expectStatus().isOk();
        assertThat(DOWNSTREAM.takeRequest(3, TimeUnit.SECONDS)).isNotNull();

        webTestClient.post().uri("/api/v1/media/products/42/images")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized();
        assertThat(DOWNSTREAM.takeRequest(300, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    @DisplayName("I11: /api/v1/products/search public, /api/v1/products/** korumalidir")
    void productRoutes_WhenSearchIsPublic_ShouldStillProtectOtherProductPaths() throws Exception {
        webTestClient.get().uri("/api/v1/products/123")
                .exchange()
                .expectStatus().isUnauthorized();

        assertThat(DOWNSTREAM.takeRequest(300, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    @DisplayName("I12: Rol claim'i olmayan gecerli token'da X-User-Role header'i downstream'e eklenmez")
    void protectedRoute_WhenTokenHasNoRoleClaim_ShouldNotSendRoleHeader() throws Exception {
        DOWNSTREAM.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        webTestClient.get().uri("/api/orders/mine")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestTokens.validWithoutRole("user-9"))
                .exchange()
                .expectStatus().isOk();

        RecordedRequest recorded = DOWNSTREAM.takeRequest(3, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getHeader("X-User-Id")).isEqualTo("user-9");
        assertThat(recorded.getHeader("X-User-Role")).isNull();
    }
}
