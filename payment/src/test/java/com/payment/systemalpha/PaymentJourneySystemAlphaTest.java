package com.payment.systemalpha;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Katman: SYSTEM ALPHA - uçtan uca, black-box.
 * <p>
 * Akış: {@code order.approved} olayı bağımsız bir AMQP istemcisiyle yayınlanır ->
 * ödeme çekilir -> durum yalnızca public HTTP API'den okunur -> iade talep edilir ->
 * yönetici iadeyi onaylar -> son durum yine API'den doğrulanır.
 * <p>
 * Hiçbir uygulama bean'i inject EDİLMEZ (yalnızca HTTP istemcisi).
 * <p>
 * <b>ÇALIŞTIRMA ÖNKOŞULU: Docker.</b>
 */
@EnabledIf("com.payment.support.DockerAvailability#isDockerAvailable")
@DisplayName("SYSTEM ALPHA - Ödeme ve iade yolculuğu (yalnızca public HTTP API)")
class PaymentJourneySystemAlphaTest extends AbstractPaymentSystemAlphaTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final ParameterizedTypeReference<List<Map<String, Object>>> PAYMENT_LIST =
            new ParameterizedTypeReference<>() {
            };

    private List<Map<String, Object>> paymentsOfOrder(UUID orderId, UUID storeUserId) {
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "/api/payments?orderId=" + orderId, HttpMethod.GET,
                new HttpEntity<>(headers(storeUserId, "STORE")), PAYMENT_LIST);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private List<Map<String, Object>> myPayments(UUID customerId) {
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "/api/payments/me", HttpMethod.GET,
                new HttpEntity<>(headers(customerId, "CUSTOMER")), PAYMENT_LIST);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    @Test
    @DisplayName("A1: Uçtan uca - order.approved yayınlanır, ödeme çekilir, API'den okunur, iade edilir")
    void endToEnd_OrderApprovedToRefunded_ShouldBeObservableOnlyThroughPublicApi() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID storeUserId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();

        publishOrderApprovedEvent(String.format(
                "{\"orderId\":\"%s\", \"customerId\":\"%s\", \"amount\":450.75, \"status\":\"APPROVED\"}",
                orderId, customerId));

        // 1) Ödeme çekildi mi? (yalnızca HTTP'den gözlemlenir)
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<Map<String, Object>> payments = paymentsOfOrder(orderId, storeUserId);
            assertThat(payments).hasSize(1);
            assertThat(payments.get(0)).containsEntry("status", "COMPLETED");
        });

        Map<String, Object> payment = paymentsOfOrder(orderId, storeUserId).get(0);
        UUID paymentId = UUID.fromString((String) payment.get("id"));
        assertThat(new BigDecimal(payment.get("amount").toString()))
                .isEqualByComparingTo(new BigDecimal("450.75"));

        // 2) Müşteri iade talep eder
        ResponseEntity<Void> refundRequest = restTemplate.exchange(
                "/api/payments/" + paymentId + "/refund-request", HttpMethod.POST,
                new HttpEntity<>("{\"reason\":\"ürün hasarlı geldi\"}", headers(customerId, "CUSTOMER")),
                Void.class);
        assertThat(refundRequest.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(myPayments(customerId).get(0)).containsEntry("status", "REFUND_REQUESTED"));

        // 3) Yönetici iadeyi onaylar
        ResponseEntity<Void> approve = restTemplate.exchange(
                "/api/payments/" + paymentId + "/refund-approve", HttpMethod.PATCH,
                new HttpEntity<>(headers(adminUserId, "ADMIN")), Void.class);
        assertThat(approve.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 4) Son durum yine yalnızca API'den doğrulanır
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Map<String, Object> refunded = myPayments(customerId).get(0);
            assertThat(refunded).containsEntry("status", "REFUNDED");
            assertThat(new BigDecimal(refunded.get("amount").toString()))
                    .as("iade edilen tutar ödenen tutarla aynı olmalı")
                    .isEqualByComparingTo(new BigDecimal("450.75"));
        });
    }

    @Test
    @DisplayName("A2: Uçtan uca - Aynı order.approved olayı iki kez yayınlanırsa müşteriden İKİ KEZ para çekilmez")
    void endToEnd_WhenSameOrderApprovedEventIsPublishedTwice_ShouldChargeOnlyOnce() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID storeUserId = UUID.randomUUID();
        String payload = String.format(
                "{\"orderId\":\"%s\", \"customerId\":\"%s\", \"amount\":120.00, \"status\":\"APPROVED\"}",
                orderId, customerId);

        publishOrderApprovedEvent(payload);
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(paymentsOfOrder(orderId, storeUserId)).hasSize(1));

        publishOrderApprovedEvent(payload);

        await().during(Duration.ofSeconds(5)).atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(paymentsOfOrder(orderId, storeUserId))
                        .as("tekrarlanan olay ikinci bir tahsilat üretmemeli")
                        .hasSize(1));
    }

    @Test
    @DisplayName("A3: Uçtan uca - Başka müşteri iade talebinde bulunamaz, ödeme COMPLETED kalır (IDOR)")
    void endToEnd_WhenAnotherCustomerRequestsRefund_ShouldNotChangePaymentStatus() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID storeUserId = UUID.randomUUID();

        publishOrderApprovedEvent(String.format(
                "{\"orderId\":\"%s\", \"customerId\":\"%s\", \"amount\":80.00, \"status\":\"APPROVED\"}",
                orderId, customerId));

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(paymentsOfOrder(orderId, storeUserId)).hasSize(1));

        UUID paymentId = UUID.fromString((String) paymentsOfOrder(orderId, storeUserId).get(0).get("id"));

        ResponseEntity<String> attack = restTemplate.exchange(
                "/api/payments/" + paymentId + "/refund-request", HttpMethod.POST,
                new HttpEntity<>("{\"reason\":\"benim değil ama denerim\"}",
                        headers(UUID.randomUUID(), "CUSTOMER")), String.class);

        assertThat(attack.getStatusCode().is2xxSuccessful())
                .as("başkasının ödemesi için iade talebi BAŞARILI olmamalı")
                .isFalse();
        assertThat(paymentsOfOrder(orderId, storeUserId).get(0)).containsEntry("status", "COMPLETED");
    }

    @Test
    @DisplayName("A4: Uçtan uca - Kimliksiz istekler public API tarafından reddedilir")
    void endToEnd_WhenNoToken_ShouldRejectEveryEndpoint() {
        assertThat(restTemplate.getForEntity("/api/payments/me", String.class)
                .getStatusCode().is2xxSuccessful()).isFalse();
        assertThat(restTemplate.getForEntity("/api/payments", String.class)
                .getStatusCode().is2xxSuccessful()).isFalse();
        assertThat(restTemplate.exchange("/api/payments/" + UUID.randomUUID() + "/refund-approve",
                HttpMethod.PATCH, HttpEntity.EMPTY, String.class)
                .getStatusCode().is2xxSuccessful()).isFalse();
    }

    @Test
    @DisplayName("A5: Uçtan uca - Yönetici olmayan kullanıcı iade onaylayamaz")
    void endToEnd_WhenNonAdminApprovesRefund_ShouldBeRejected() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/payments/" + UUID.randomUUID() + "/refund-approve", HttpMethod.PATCH,
                new HttpEntity<>(headers(UUID.randomUUID(), "CUSTOMER")), String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isFalse();
    }
}
