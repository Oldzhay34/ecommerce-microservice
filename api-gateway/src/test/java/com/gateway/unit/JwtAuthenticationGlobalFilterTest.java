package com.gateway.unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.filter.JwtAuthenticationGlobalFilter;
import com.gateway.security.JwtValidator;
import com.gateway.support.JwtTestTokens;
import com.gateway.support.TestObjectMappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtAuthenticationGlobalFilter whitebox unit testleri.
 * Ag yok: MockServerWebExchange + elle yazilmis kayit tutan bir GatewayFilterChain.
 */
class JwtAuthenticationGlobalFilterTest {

    private JwtAuthenticationGlobalFilter filter;
    private RecordingChain chain;
    private ObjectMapper objectMapper;

    /** chain.filter(...) cagrilirsa gelen exchange'i saklar; cagrilmazsa null kalir. */
    private static final class RecordingChain implements GatewayFilterChain {
        private final List<ServerWebExchange> invocations = new ArrayList<>();

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            invocations.add(exchange);
            return Mono.empty();
        }

        boolean wasCalled() {
            return !invocations.isEmpty();
        }

        ServerWebExchange lastExchange() {
            return invocations.get(invocations.size() - 1);
        }

        HttpHeaders forwardedHeaders() {
            return lastExchange().getRequest().getHeaders();
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = TestObjectMappers.springBootLike();
        filter = new JwtAuthenticationGlobalFilter(new JwtValidator(JwtTestTokens.SECRET), objectMapper);
        chain = new RecordingChain();
    }

    // --- Public path bypass -------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/"
    })
    @DisplayName("U1: filter - /api/v1/auth/ prefixli path'ler token olmadan gecer")
    void filter_WhenPathIsAuthPrefix_ShouldBypassJwtValidation(String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(path));

        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("U2: filter - /api/v1/products/search tam eslesmesi public, alt path'i public DEGIL")
    void filter_WhenPathIsProductSearch_ShouldBypassButSubPathShouldNot() {
        MockServerWebExchange publicExchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products/search"));
        filter.filter(publicExchange, chain).block();
        assertThat(chain.wasCalled()).isTrue();

        RecordingChain second = new RecordingChain();
        MockServerWebExchange protectedExchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products/search/advanced"));
        filter.filter(protectedExchange, second).block();

        assertThat(second.wasCalled()).isFalse();
        assertThat(protectedExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("U3: filter - CORS preflight (OPTIONS) istegi token aranmadan gecer")
    void filter_WhenMethodIsOptions_ShouldBypassJwtValidation() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.options("/api/orders/1"));

        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "GET,  /api/v1/media/products/42/images,        true",
            "POST, /api/v1/media/products/42/images,        false",
            "POST, /api/v1/media/products/images/batch,     true",
            "GET,  /api/v1/media/products/images/batch,     false",
            "GET,  /api/reviews/product/7,                  true",
            "POST, /api/reviews/product/7,                  false",
            "GET,  /api/reviews/9,                          false"
    })
    @DisplayName("U4: filter - Method+path ciftine gore public whitelist dogru calisir")
    void filter_WhenMethodAndPathCombinationVaries_ShouldApplyWhitelistPerMethod(
            String method, String path, boolean expectedPublic) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.valueOf(method), path).build());

        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isEqualTo(expectedPublic);
        if (!expectedPublic) {
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    @DisplayName("U5: filter - Public path'te bile client'in gonderdigi sahte X-User-* header'lari temizlenir")
    void filter_WhenPublicPathHasSpoofedIdentityHeaders_ShouldStripThem() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/products/search")
                        .header("X-User-Id", "999")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isTrue();
        assertThat(chain.forwardedHeaders().getFirst("X-User-Id")).isNull();
        assertThat(chain.forwardedHeaders().getFirst("X-User-Role")).isNull();
    }

    // --- Authorization header guard'lari ------------------------------------

    @Test
    @DisplayName("U6: filter - Authorization header hic yoksa 401 doner ve downstream cagrilmaz")
    void filter_WhenAuthorizationHeaderMissing_ShouldReturnUnauthorized() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders/1"));

        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "Basic dXNlcjpwYXNz",
            "bearer lowercase-prefix",
            "Token abc.def.ghi",
            "Bearer",
            "BearerNoSpace.token"
    })
    @DisplayName("U7: filter - 'Bearer ' oneki olmayan Authorization degerleri 401 ile reddedilir")
    void filter_WhenAuthorizationHeaderHasNoBearerPrefix_ShouldReturnUnauthorized(String headerValue) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/1")
                        .header(HttpHeaders.AUTHORIZATION, headerValue)
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("U8: filter - 401 govdesi ErrorResponse formatindadir ve istek path'ini icerir")
    void filter_WhenUnauthorized_ShouldWriteErrorResponseBody() throws Exception {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders/1"));

        filter.filter(exchange, chain).block();

        JsonNode body = objectMapper.readTree(readBody(exchange));
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("path").asText()).isEqualTo("/api/orders/1");
        assertThat(body.get("message").asText()).isNotBlank();
        assertThat(body.hasNonNull("timestamp")).isTrue();
    }

    @Test
    @DisplayName("U9: filter - 401 mesaji dahili exception detayini sizdirmaz")
    void filter_WhenTokenIsInvalid_ShouldNotLeakInternalExceptionDetails() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestTokens.signedWithWrongKey("hacker"))
                        .build());

        filter.filter(exchange, chain).block();

        String body = readBody(exchange);
        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("status").asInt()).isEqualTo(401);
        assertThat(body)
                .doesNotContain("JwtException")
                .doesNotContain("SignatureException")
                .doesNotContain("io.jsonwebtoken")
                .doesNotContain(JwtTestTokens.SECRET);
    }

    // --- Token dogrulama dallari --------------------------------------------

    @Test
    @DisplayName("U10: filter - Suresi dolmus token 401 ile reddedilir")
    void filter_WhenTokenIsExpired_ShouldReturnUnauthorized() {
        MockServerWebExchange exchange = bearer("/api/orders/1", JwtTestTokens.expired("user-1"));

        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("U11: filter - Yanlis secret ile imzalanmis token 401 ile reddedilir")
    void filter_WhenTokenSignedWithWrongSecret_ShouldReturnUnauthorized() {
        MockServerWebExchange exchange = bearer("/api/orders/1", JwtTestTokens.signedWithWrongKey("hacker"));

        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("U12: filter - Bozuk formatli token 401 ile reddedilir")
    void filter_WhenTokenIsMalformed_ShouldReturnUnauthorized() {
        MockServerWebExchange exchange = bearer("/api/orders/1", JwtTestTokens.malformed());

        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("U13: filter - 'Bearer ' sonrasi bos deger 401 ile reddedilir")
    void filter_WhenBearerTokenIsEmpty_ShouldReturnUnauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer   ")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- Basarili akis: downstream'e giden header'lar -----------------------

    @Test
    @DisplayName("U14: filter - Gecerli token'da X-User-Id ve X-User-Role downstream'e eklenir, Authorization korunur")
    void filter_WhenTokenIsValid_ShouldEnrichHeadersAndKeepAuthorization() {
        String token = JwtTestTokens.valid("user-77", "ROLE_STORE");
        MockServerWebExchange exchange = bearer("/api/orders/1", token);

        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isTrue();
        HttpHeaders forwarded = chain.forwardedHeaders();
        assertThat(forwarded.getFirst("X-User-Id")).isEqualTo("user-77");
        assertThat(forwarded.getFirst("X-User-Role")).isEqualTo("ROLE_STORE");
        assertThat(forwarded.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + token);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("U15: filter - Client'in gonderdigi sahte X-User-Id gecerli token ile EZILIR (yetki yukseltme engellenir)")
    void filter_WhenClientSpoofsIdentityHeaders_ShouldOverwriteThemWithTokenValues() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + JwtTestTokens.valid("user-5", "ROLE_USER"))
                        .header("X-User-Id", "1")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .build());

        filter.filter(exchange, chain).block();

        HttpHeaders forwarded = chain.forwardedHeaders();
        assertThat(forwarded.get("X-User-Id")).containsExactly("user-5");
        assertThat(forwarded.get("X-User-Role")).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("U16: filter - Rol claim'i yoksa X-User-Role header'i hic eklenmez")
    void filter_WhenRoleClaimMissing_ShouldNotAddRoleHeader() {
        MockServerWebExchange exchange = bearer("/api/orders/1", JwtTestTokens.validWithoutRole("user-9"));

        filter.filter(exchange, chain).block();

        assertThat(chain.forwardedHeaders().getFirst("X-User-Id")).isEqualTo("user-9");
        assertThat(chain.forwardedHeaders().containsKey("X-User-Role")).isFalse();
    }

    @Test
    @DisplayName("U17: filter - Subject claim'i olmayan token'da X-User-Id header'i eklenmez")
    void filter_WhenSubjectClaimMissing_ShouldNotAddUserIdHeader() {
        MockServerWebExchange exchange = bearer("/api/orders/1", JwtTestTokens.withoutSubject());

        filter.filter(exchange, chain).block();

        assertThat(chain.wasCalled()).isTrue();
        assertThat(chain.forwardedHeaders().containsKey("X-User-Id")).isFalse();
    }

    @Test
    @DisplayName("U18: getOrder - Filtre routing'den once, -100 onceliginde calisir")
    void getOrder_ShouldRunBeforeRoutingFilters() {
        assertThat(filter.getOrder()).isEqualTo(-100);
    }

    // --- yardimcilar --------------------------------------------------------

    private static MockServerWebExchange bearer(String path, String token) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
    }

    private static String readBody(MockServerWebExchange exchange) {
        return exchange.getResponse().getBodyAsString()
                .defaultIfEmpty("")
                .block();
    }
}
