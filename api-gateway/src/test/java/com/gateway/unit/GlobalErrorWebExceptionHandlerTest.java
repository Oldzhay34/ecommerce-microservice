package com.gateway.unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.exception.GlobalErrorWebExceptionHandler;
import com.gateway.support.TestObjectMappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GlobalErrorWebExceptionHandler whitebox unit testleri:
 * status eslemeleri, ErrorResponse govdesi ve bilgi sizintisi kontrolu.
 */
class GlobalErrorWebExceptionHandlerTest {

    private GlobalErrorWebExceptionHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = TestObjectMappers.springBootLike();
        handler = new GlobalErrorWebExceptionHandler(objectMapper);
    }

    @Test
    @DisplayName("U1: handle - Rate limit (429) ResponseStatusException'i 429 ve Turkce limit mesajina cevirir")
    void handle_WhenTooManyRequests_ShouldReturn429WithRateLimitMessage() throws Exception {
        MockServerWebExchange exchange = exchange("/api/orders");

        handler.handle(exchange, new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        JsonNode body = body(exchange);
        assertThat(body.get("status").asInt()).isEqualTo(429);
        assertThat(body.get("message").asText()).contains("Istek limiti asildi");
        assertThat(body.get("path").asText()).isEqualTo("/api/orders");
    }

    @Test
    @DisplayName("U2: handle - ResponseStatusException status'u aynen korunur (404 route bulunamadi)")
    void handle_WhenResponseStatusException_ShouldPreserveStatusCode() throws Exception {
        MockServerWebExchange exchange = exchange("/bilinmeyen/yol");

        handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body(exchange).get("status").asInt()).isEqualTo(404);
    }

    @Test
    @DisplayName("U3: handle - ResponseStatusException reason'i varsa mesaj olarak kullanilir")
    void handle_WhenResponseStatusExceptionHasReason_ShouldUseItAsMessage() throws Exception {
        MockServerWebExchange exchange = exchange("/api/orders");

        handler.handle(exchange, new ResponseStatusException(HttpStatus.BAD_REQUEST, "Eksik parametre")).block();

        assertThat(body(exchange).get("message").asText()).isEqualTo("Eksik parametre");
    }

    @Test
    @DisplayName("U4: handle - Downstream'e baglanilamiyorsa 503 doner")
    void handle_WhenConnectException_ShouldReturnServiceUnavailable() throws Exception {
        MockServerWebExchange exchange = exchange("/api/orders");

        handler.handle(exchange, new ConnectException("Connection refused: order-service:8081")).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(body(exchange).get("message").asText()).contains("Hedef servise su anda ulasilamiyor");
    }

    @Test
    @DisplayName("U5: handle - IOException de 503'e eslenir")
    void handle_WhenIOException_ShouldReturnServiceUnavailable() {
        MockServerWebExchange exchange = exchange("/api/carts");

        handler.handle(exchange, new IOException("connection reset")).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("U6: handle - Sinif adi 'Timeout' iceren hatalar 503'e eslenir")
    void handle_WhenTimeoutException_ShouldReturnServiceUnavailable() {
        MockServerWebExchange exchange = exchange("/api/payments");

        handler.handle(exchange, new TimeoutException("read timed out")).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("U7: handle - Beklenmeyen hata 500'e eslenir ve exception detayini SIZDIRMAZ")
    void handle_WhenUnexpectedException_ShouldReturn500WithoutLeakingDetails() throws Exception {
        MockServerWebExchange exchange = exchange("/api/orders");
        RuntimeException secretive = new IllegalStateException(
                "jdbc:postgresql://db:5432/orders user=admin password=s3cr3t");

        handler.handle(exchange, secretive).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        String raw = rawBody(exchange);
        assertThat(raw)
                .doesNotContain("password")
                .doesNotContain("jdbc:")
                .doesNotContain("IllegalStateException");
        assertThat(body(exchange).get("message").asText())
                .isEqualTo("Gateway tarafinda beklenmeyen bir hata olustu.");
    }

    @Test
    @DisplayName("U8: handle - Yanit govdesi application/json ve tam ErrorResponse alanlarini icerir")
    void handle_WhenAnyError_ShouldWriteJsonErrorResponseWithAllFields() throws Exception {
        MockServerWebExchange exchange = exchange("/api/reviews/3");

        handler.handle(exchange, new RuntimeException("bir sey")).block();

        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        JsonNode body = body(exchange);
        assertThat(body.hasNonNull("timestamp")).isTrue();
        assertThat(body.hasNonNull("status")).isTrue();
        assertThat(body.hasNonNull("message")).isTrue();
        assertThat(body.get("path").asText()).isEqualTo("/api/reviews/3");
    }

    @Test
    @DisplayName("U9: handle - Yanit zaten commit edilmisse govdeyi tekrar yazmaz, hatayi geri firlatir")
    void handle_WhenResponseAlreadyCommitted_ShouldRethrowError() {
        MockServerWebExchange exchange = exchange("/api/orders");
        exchange.getResponse().setComplete().block();

        RuntimeException ex = new RuntimeException("gec kalan hata");

        StepVerifier.create(handler.handle(exchange, ex))
                .expectErrorMatches(t -> t == ex)
                .verify();
    }

    @Test
    @DisplayName("U10: @Order - Spring Boot'un varsayilan hata isleyicisinden (-1) ONCE calisacak sekilde siralanmistir")
    void order_ShouldTakePrecedenceOverSpringBootDefaultErrorHandler() {
        Order order = GlobalErrorWebExceptionHandler.class.getAnnotation(Order.class);

        // Boot'un DefaultErrorWebExceptionHandler bean'i @Order(-1) ile kayitli oldugundan
        // esit deger belirsiz siralamaya yol acar; bizimkinin kesin olarak daha onde olmasi gerekir.
        assertThat(order).isNotNull();
        assertThat(order.value()).isLessThan(-1);
    }

    // --- yardimcilar --------------------------------------------------------

    private static MockServerWebExchange exchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path));
    }

    private String rawBody(MockServerWebExchange exchange) {
        return exchange.getResponse().getBodyAsString().defaultIfEmpty("").block();
    }

    private JsonNode body(MockServerWebExchange exchange) throws Exception {
        return objectMapper.readTree(rawBody(exchange));
    }
}
