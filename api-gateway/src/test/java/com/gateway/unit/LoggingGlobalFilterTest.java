package com.gateway.unit;

import com.gateway.filter.LoggingGlobalFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LoggingGlobalFilter whitebox unit testleri.
 * Filtre gozlemcidir: istegi/yaniti degistirmemeli, hatalari yutmamalidir.
 */
@ExtendWith(MockitoExtension.class)
class LoggingGlobalFilterTest {

    @Mock
    private GatewayFilterChain chain;

    private LoggingGlobalFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoggingGlobalFilter();
    }

    @Test
    @DisplayName("U1: filter - Zinciri aynen cagirir ve istegi degistirmez")
    void filter_WhenInvoked_ShouldDelegateToChainWithoutMutatingRequest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/1").header("X-User-Id", "user-1").build());
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
        assertThat(exchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("user-1");
    }

    @Test
    @DisplayName("U2: filter - X-User-Id header'i yokken de hatasiz tamamlanir (null guard)")
    void filter_WhenUserIdHeaderMissing_ShouldCompleteWithoutError() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/products/search"));
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
    }

    @Test
    @DisplayName("U3: filter - Yanit status'u set edilmisse akis yine sorunsuz tamamlanir")
    void filter_WhenResponseStatusIsSet_ShouldCompleteSuccessfully() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders/1"));
        exchange.getResponse().setStatusCode(HttpStatus.CREATED);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("U4: filter - Downstream hatasini yutmaz, cagirana aynen iletir")
    void filter_WhenChainFails_ShouldPropagateError() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders/1"));
        when(chain.filter(any())).thenReturn(Mono.error(new IllegalStateException("downstream patladi")));

        StepVerifier.create(filter.filter(exchange, chain))
                .expectErrorMessage("downstream patladi")
                .verify();
    }

    @Test
    @DisplayName("U5: getOrder - JWT filtresinden (-100) SONRA calisir ki X-User-Id cozulmus olsun")
    void getOrder_ShouldRunAfterJwtFilter() {
        assertThat(filter.getOrder()).isEqualTo(-50);
        assertThat(filter.getOrder()).isGreaterThan(-100);
    }
}
