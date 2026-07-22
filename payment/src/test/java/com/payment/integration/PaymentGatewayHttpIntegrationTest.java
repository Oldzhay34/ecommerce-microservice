package com.payment.integration;

import com.payment.application.port.out.PaymentChargeCommand;
import com.payment.application.port.out.PaymentCommandPort;
import com.payment.application.port.out.PaymentGatewayPort;
import com.payment.application.port.out.PaymentGatewayResult;
import com.payment.application.port.out.PaymentQueryPort;
import com.payment.application.usecase.PaymentCommandUseCaseImpl;
import com.payment.domain.exception.PaymentGatewayNotConfiguredException;
import com.payment.domain.model.Payment;
import com.payment.infrastructure.config.RestTemplateConfig;
import com.payment.infrastructure.gateway.iyzico.IyzicoPaymentGatewayAdapter;
import com.payment.support.PaymentTestFixtures;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Katman: INTEGRATION - GERÇEK bir soket üzerinden harici ödeme sağlayıcısı davranışı.
 * <p>
 * Uygulamanın {@link RestTemplateConfig} ile ürettiği GERÇEK RestTemplate,
 * MockWebServer'a karşı çalıştırılır: 5xx, timeout, bozuk/eksik JSON ve başarısız
 * ödeme yanıtı senaryolarının hepsinde istemcinin SESSİZCE BAŞARILI SAYMADIĞI
 * doğrulanır.
 * <p>
 * DİKKAT: {@link IyzicoPaymentGatewayAdapter} production'da henüz bir HTTP
 * entegrasyonu İÇERMEZ; her çağrıda {@link PaymentGatewayNotConfiguredException}
 * fırlatır. Testler bu gerçeği kabul eder ve olmayan bir davranışı taklit etmez -
 * adapter için doğrulanan şey "harici sisteme hiç istek gitmez"dir.
 */
@DisplayName("Payment Service - Integration: harici gateway HTTP davranışı (MockWebServer)")
class PaymentGatewayHttpIntegrationTest {

    private MockWebServer server;
    private RestTemplate restTemplate;

    /** Timeout senaryosunu makul sürede kanıtlayabilmek için kısa okuma zaman aşımı. */
    private static final long READ_TIMEOUT_MS = 400;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        restTemplate = new RestTemplateConfig().restTemplate(1000, READ_TIMEOUT_MS);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    private String url(String path) {
        return server.url(path).toString();
    }

    // ---------- RestTemplateConfig'in ürettiği istemcinin hata davranışı ----------

    @Test
    @DisplayName("I34: Gateway 500 dönerse istemci HttpServerErrorException fırlatır (yanıt başarılı sayılmaz)")
    void gatewayClient_WhenProviderReturns500_ShouldThrowServerError() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\":\"internal\"}"));

        assertThatThrownBy(() -> restTemplate.postForObject(url("/payment/auth"), Map.of("amount", "100.00"), Map.class))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    @DisplayName("I35: Gateway 503 dönerse istemci hata fırlatır ve gövde sessizce yok sayılmaz")
    void gatewayClient_WhenProviderIsUnavailable_ShouldThrowServerError() {
        server.enqueue(new MockResponse().setResponseCode(503));

        assertThatThrownBy(() -> restTemplate.postForObject(url("/payment/auth"), Map.of(), Map.class))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    @DisplayName("I36: Gateway 401 dönerse (hatalı API anahtarı) istemci istemci-hatası fırlatır")
    void gatewayClient_WhenCredentialsAreRejected_ShouldThrowClientError() {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"errorCode\":\"1001\"}"));

        assertThatThrownBy(() -> restTemplate.postForObject(url("/payment/auth"), Map.of(), Map.class))
                .isInstanceOf(HttpClientErrorException.class);
    }

    /**
     * REGRESYON (LATENT KALIP DÜZELTMESİ): RestTemplateConfig çıplak
     * {@code new RestTemplate()} dönüyordu; varsayılan connect/read timeout SINIRSIZDIR.
     * Yanıt vermeyen bir gateway, tahsilat işleminin thread'ini ve @Transactional
     * DB bağlantısını süresiz bloke ederdi.
     */
    @Test
    @DisplayName("I37: Gateway yanıt vermezse istemci yapılandırılan süre içinde timeout verir (sonsuza kadar beklemez)")
    void gatewayClient_WhenProviderHangs_ShouldTimeOutInsteadOfBlockingForever() {
        server.enqueue(new MockResponse().setBody("{}").setBodyDelay(5, TimeUnit.SECONDS));

        long startedAt = System.currentTimeMillis();
        assertThatThrownBy(() -> restTemplate.postForObject(url("/payment/auth"), Map.of(), Map.class))
                .isInstanceOf(ResourceAccessException.class);

        assertThat(System.currentTimeMillis() - startedAt)
                .as("okuma zaman aşımı yapılandırıldığı için istek erken sonlanmalı")
                .isLessThan(4000L);
    }

    @Test
    @DisplayName("I38: Gateway bozuk JSON dönerse ayrıştırma hatası fırlatılır, yarım yanıt kabul edilmez")
    void gatewayClient_WhenResponseBodyIsMalformedJson_ShouldThrowRestClientException() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"status\":\"succ"));

        assertThatThrownBy(() -> restTemplate.postForObject(url("/payment/auth"), Map.of(), Map.class))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    @DisplayName("I39: Gateway boş gövdeyle 200 dönerse sonuç null olur, 'başarılı ödeme' varsayılmaz")
    void gatewayClient_WhenResponseBodyIsEmpty_ShouldNotBeTreatedAsSuccessfulPayment() {
        server.enqueue(new MockResponse().setResponseCode(200));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url("/payment/auth"), Map.of(), Map.class);

        assertThat(response).as("gövdesiz 200 tahsilat kanıtı değildir").isNull();
    }

    @Test
    @DisplayName("I40: Gateway başarısız ödeme yanıtı dönerse gövde okunur ve durum 'failure' olarak görülür")
    void gatewayClient_WhenProviderDeclinesPayment_ShouldExposeFailureStatusInBody() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"status\":\"failure\",\"errorMessage\":\"Yetersiz bakiye\"}"));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url("/payment/auth"), Map.of(), Map.class);

        assertThat(response).containsEntry("status", "failure");
    }

    // ---------- Adapter'ın gerçek soket karşısındaki davranışı ----------

    @Test
    @DisplayName("I41: IyzicoPaymentGatewayAdapter - Sağlayıcı 200/başarılı yanıt verse bile HİÇ istek gönderilmez")
    void iyzicoAdapter_ShouldNotSendAnyRequestEvenWhenProviderWouldSucceed() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"status\":\"success\"}"));
        IyzicoPaymentGatewayAdapter adapter = new IyzicoPaymentGatewayAdapter(restTemplate, "key", "secret");

        assertThatThrownBy(() -> adapter.charge(
                PaymentTestFixtures.chargeCommand(UUID.randomUUID(), UUID.randomUUID(), "100.00")))
                .isInstanceOf(PaymentGatewayNotConfiguredException.class);

        assertThat(server.getRequestCount())
                .as("yapılandırılmamış adapter sağlayıcıya istek göndermemeli")
                .isZero();
    }

    /**
     * Ağ hatasının para durumunu tutarsız bırakmadığının kanıtı: gateway HTTP
     * hatası fırlattığında use case istisnayı yukarı taşır (@Transactional geri alır)
     * ve COMPLETED durumu ile PaymentCompletedEvent ASLA üretilmez.
     */
    @Test
    @DisplayName("I42: Gateway 500 verdiğinde ödeme COMPLETED'e geçmez ve outbox olayı yazılmaz")
    void paymentFlow_WhenGatewayReturnsServerError_ShouldNotLeaveMoneyStateInconsistent() {
        server.enqueue(new MockResponse().setResponseCode(500));

        PaymentCommandPort commandPort = mock(PaymentCommandPort.class);
        PaymentQueryPort queryPort = mock(PaymentQueryPort.class);
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        when(commandPort.existsPaymentForOrder(orderId)).thenReturn(false);
        when(commandPort.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment argument = invocation.getArgument(0);
            if (argument.getId() == null) {
                argument.setId(UUID.randomUUID());
            }
            return argument;
        });

        // Gerçek HTTP yapan bir gateway: sağlayıcı 500 dönünce istisna fırlatır.
        PaymentGatewayPort httpGateway = new PaymentGatewayPort() {
            @Override
            public PaymentGatewayResult charge(PaymentChargeCommand command) {
                restTemplate.postForObject(url("/payment/auth"), Map.of("amount", command.amount()), Map.class);
                throw new IllegalStateException("buraya ulaşılmamalı");
            }
        };

        PaymentCommandUseCaseImpl useCase = new PaymentCommandUseCaseImpl(commandPort, queryPort, httpGateway);

        assertThatThrownBy(() -> useCase.processOrderApproved(orderId, customerId, PaymentTestFixtures.TRY_100))
                .isInstanceOf(HttpServerErrorException.class);

        verify(commandPort, never()).saveOutboxEvent(anyString(), anyString(), anyString(), anyString());
        assertThat(server.getRequestCount()).isEqualTo(1);
    }
}
