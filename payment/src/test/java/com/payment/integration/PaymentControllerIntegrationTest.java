package com.payment.integration;

import com.payment.api.controller.PaymentController;
import com.payment.application.port.in.PaymentCommandUseCase;
import com.payment.application.port.in.PaymentQueryUseCase;
import com.payment.domain.exception.InvalidRefundException;
import com.payment.domain.exception.PaymentNotFoundException;
import com.payment.domain.model.Payment;
import com.payment.domain.model.PaymentStatus;
import com.payment.infrastructure.security.JwtAuthFilter;
import com.payment.infrastructure.security.JwtTokenProvider;
import com.payment.infrastructure.security.SecurityConfig;
import com.payment.support.JwtTestTokens;
import com.payment.support.PaymentTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Katman: INTEGRATION (slice) - GERÇEK Spring Security filtre zinciri
 * ({@code addFilters = true}) + gerçek {@link JwtAuthFilter} /
 * {@link JwtTokenProvider} / {@link SecurityConfig} ile PaymentController'ın HTTP
 * sözleşmesi doğrulanır. Token'lar test tarafında GERÇEKTEN imzalanır. Use case'ler
 * {@code @MockitoBean} ile izole edilir: burada test edilen iş mantığı değil,
 * durum kodları / yetkilendirme / serileştirmedir.
 *
 * <p><b>401 vs 403 NOTU:</b> SecurityConfig'te özel bir {@code authenticationEntryPoint}
 * TANIMLI DEĞİLDİR; bu yüzden Spring Security'nin varsayılanı devrededir ve kimliksiz
 * istek de yetkisiz istek de <b>403</b> üretir (api-gateway 401 dönerken). Bu
 * tutarsızlık bilinçli olarak DEĞİŞTİRİLMEMİŞTİR; testler gözlemlenen gerçek
 * davranışı sabitler.
 */
@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtTokenProvider.class})
@TestPropertySource(properties = "jwt.secret=" + JwtTestTokens.SECRET)
@DisplayName("Payment Service - Integration: PaymentController (gerçek güvenlik filtre zinciri)")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentCommandUseCase commandUseCase;

    @MockitoBean
    private PaymentQueryUseCase queryUseCase;

    private static Payment payment(UUID id, UUID orderId, UUID customerId, String amount, PaymentStatus status) {
        return PaymentTestFixtures.payment(id, orderId, customerId, amount, status);
    }

    // ==================== GET /api/payments/me ====================

    @Nested
    @DisplayName("GET /api/payments/me - müşterinin kendi ödemeleri")
    class MyPayments {

        @Test
        @DisplayName("I1: GET /me - CUSTOMER kendi ödemelerini görür, tutar JSON'da sayısal olarak döner")
        void getMyPayments_WhenCustomer_ShouldReturn200WithOwnPayments() throws Exception {
            UUID customerId = UUID.randomUUID();
            UUID paymentId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            when(queryUseCase.getPaymentsByCustomerId(customerId))
                    .thenReturn(List.of(payment(paymentId, orderId, customerId, "1234.56", PaymentStatus.COMPLETED)));

            mockMvc.perform(get("/api/payments/me")
                            .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.customerToken(customerId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(paymentId.toString()))
                    .andExpect(jsonPath("$[0].orderId").value(orderId.toString()))
                    .andExpect(jsonPath("$[0].amount").value(1234.56))
                    .andExpect(jsonPath("$[0].status").value("COMPLETED"));
        }

        /**
         * IDOR: müşteri kimliği gövdeden/parametreden değil, YALNIZCA JWT subject'inden
         * alınır. Başka bir müşterinin kimliğini parametre olarak geçmek işe yaramaz.
         */
        @Test
        @DisplayName("I2: GET /me - Müşteri kimliği JWT subject'inden alınır, istek parametresiyle değiştirilemez (IDOR)")
        void getMyPayments_ShouldResolveCustomerIdFromJwtSubjectOnly() throws Exception {
            UUID caller = UUID.randomUUID();
            UUID victim = UUID.randomUUID();
            when(queryUseCase.getPaymentsByCustomerId(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/payments/me")
                            .param("customerId", victim.toString())
                            .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.customerToken(caller))))
                    .andExpect(status().isOk());

            verify(queryUseCase).getPaymentsByCustomerId(caller);
            verify(queryUseCase, never()).getPaymentsByCustomerId(victim);
        }

        @Test
        @DisplayName("I3: GET /me - Token olmadan erişim reddedilir (kimliksiz istek 403 döner, bkz. sınıf notu)")
        void getMyPayments_WhenAnonymous_ShouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/payments/me"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(queryUseCase);
        }

        @Test
        @DisplayName("I4: GET /me - ADMIN rolü müşteri uç noktasına erişemez")
        void getMyPayments_WhenAdmin_ShouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/payments/me")
                            .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.adminToken(UUID.randomUUID()))))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(queryUseCase);
        }

        @Test
        @DisplayName("I5: GET /me - Yanlış secret ile imzalanmış token reddedilir")
        void getMyPayments_WhenTokenSignatureIsInvalid_ShouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/payments/me")
                            .header("Authorization", JwtTestTokens.bearer(JwtTestTokens
                                    .tokenSignedWithWrongSecret(UUID.randomUUID().toString(), "CUSTOMER"))))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(queryUseCase);
        }

        @Test
        @DisplayName("I6: GET /me - Süresi dolmuş token reddedilir")
        void getMyPayments_WhenTokenIsExpired_ShouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/payments/me")
                            .header("Authorization", JwtTestTokens.bearer(JwtTestTokens
                                    .expiredToken(UUID.randomUUID().toString(), "CUSTOMER"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("I7: GET /me - Ödemesi olmayan müşteri boş dizi alır")
        void getMyPayments_WhenNoPayments_ShouldReturnEmptyArray() throws Exception {
            UUID customerId = UUID.randomUUID();
            when(queryUseCase.getPaymentsByCustomerId(customerId)).thenReturn(List.of());

            mockMvc.perform(get("/api/payments/me")
                            .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.customerToken(customerId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("I8: GET /me - Kuruşlu tutarlar JSON'a yuvarlanmadan yazılır")
        void getMyPayments_ShouldSerializeMinorUnitsWithoutRounding() throws Exception {
            UUID customerId = UUID.randomUUID();
            when(queryUseCase.getPaymentsByCustomerId(customerId)).thenReturn(List.of(
                    payment(UUID.randomUUID(), UUID.randomUUID(), customerId, "0.01", PaymentStatus.COMPLETED),
                    payment(UUID.randomUUID(), UUID.randomUUID(), customerId, "19.99", PaymentStatus.REFUNDED)));

            mockMvc.perform(get("/api/payments/me")
                            .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.customerToken(customerId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].amount").value(0.01))
                    .andExpect(jsonPath("$[1].amount").value(19.99));
        }
    }

    // ==================== GET /api/payments ====================

    @Nested
    @DisplayName("GET /api/payments - mağaza/yönetici sorguları")
    class AdminAndStoreQueries {

        @Test
        @DisplayName("I9: GET /api/payments - ADMIN tüm ödemeleri görür")
        void getPayments_WhenAdmin_ShouldReturnAllPayments() throws Exception {
            when(queryUseCase.getAllPayments()).thenReturn(List.of(
                    payment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "10.00", PaymentStatus.COMPLETED),
                    payment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "20.00", PaymentStatus.FAILED)));

            mockMvc.perform(get("/api/payments")
                            .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.adminToken(UUID.randomUUID()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));

            verify(queryUseCase).getAllPayments();
        }

        @Test
        @DisplayName("I10: GET /api/payments?orderId - STORE yalnızca belirtilen siparişin ödemelerini görür")
        void getPayments_WhenStoreWithOrderId_ShouldReturnOrderPayments() throws Exception {
            UUID orderId = UUID.randomUUID();
            when(queryUseCase.getPaymentsByOrderId(orderId)).thenReturn(List.of(
                    payment(UUID.randomUUID(), orderId, UUID.randomUUID(), "10.00", PaymentStatus.COMPLETED)));

            mockMvc.perform(get("/api/payments")
                            .param("orderId", orderId.toString())
                            .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.storeToken(UUID.randomUUID()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(queryUseCase, never()).getAllPayments();
        }

        @Test
        @DisplayName("I11: GET /api/payments - STORE orderId vermezse 400 alır (tüm ödemeleri listeleyemez)")
        void getPayments_WhenStoreWithoutOrderId_ShouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/api/payments")
                            .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.storeToken(UUID.randomUUID()))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(queryUseCase);
        }

        @Test
        @DisplayName("I12: GET /api/payments - CUSTOMER rolü mağaza/yönetici uç noktasına erişemez")
        void getPayments_WhenCustomer_ShouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/payments")
                            .header("Authorization", JwtTestTokens.bearer(
                                    JwtTestTokens.customerToken(UUID.randomUUID()))))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(queryUseCase);
        }

        @Test
        @DisplayName("I13: GET /api/payments - Kimliksiz istek reddedilir")
        void getPayments_WhenAnonymous_ShouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/payments"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("I14: GET /api/payments - Geçersiz UUID formatlı orderId 400 üretir")
        void getPayments_WhenOrderIdIsNotUuid_ShouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/api/payments")
                            .param("orderId", "not-a-uuid")
                            .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.storeToken(UUID.randomUUID()))))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== POST /{id}/refund-request ====================

    @Nested
    @DisplayName("POST /api/payments/{id}/refund-request - iade talebi")
    class RefundRequestEndpoint {

        @Test
        @DisplayName("I15: POST /refund-request - CUSTOMER iade talebi oluşturur, 202 döner")
        void requestRefund_WhenCustomer_ShouldReturn202AndDelegateWithJwtSubject() throws Exception {
            UUID customerId = UUID.randomUUID();
            UUID paymentId = UUID.randomUUID();

            mockMvc.perform(post("/api/payments/{id}/refund-request", paymentId)
                            .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.customerToken(customerId)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"ürün hasarlı geldi\"}"))
                    .andExpect(status().isAccepted());

            verify(commandUseCase).requestRefund(paymentId, customerId, "ürün hasarlı geldi");
        }

        @Test
        @DisplayName("I16: POST /refund-request - reason alanı yoksa 400 üretilir (validation NPE'ye dönüşmez)")
        void requestRefund_WhenReasonIsMissing_ShouldReturnBadRequest() throws Exception {
            mockMvc.perform(post("/api/payments/{id}/refund-request", UUID.randomUUID())
                            .header("Authorization", JwtTestTokens.bearer(
                                    JwtTestTokens.customerToken(UUID.randomUUID())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(commandUseCase);
        }

        @Test
        @DisplayName("I17: POST /refund-request - reason boşluktan ibaretse 400 üretilir")
        void requestRefund_WhenReasonIsBlank_ShouldReturnBadRequest() throws Exception {
            mockMvc.perform(post("/api/payments/{id}/refund-request", UUID.randomUUID())
                            .header("Authorization", JwtTestTokens.bearer(
                                    JwtTestTokens.customerToken(UUID.randomUUID())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"   \"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(commandUseCase);
        }

        @Test
        @DisplayName("I18: POST /refund-request - ADMIN rolü müşteri iade talebi oluşturamaz")
        void requestRefund_WhenAdmin_ShouldBeForbidden() throws Exception {
            mockMvc.perform(post("/api/payments/{id}/refund-request", UUID.randomUUID())
                            .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.adminToken(UUID.randomUUID())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"x\"}"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(commandUseCase);
        }

        @Test
        @DisplayName("I19: POST /refund-request - Kimliksiz istek reddedilir")
        void requestRefund_WhenAnonymous_ShouldBeForbidden() throws Exception {
            mockMvc.perform(post("/api/payments/{id}/refund-request", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"x\"}"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(commandUseCase);
        }

        /**
         * Servis genelinde bir {@code @RestControllerAdvice} YOKTUR; domain
         * istisnaları işlenmeden dışarı çıkar. MockMvc bunu sarmalayarak fırlatır -
         * gerçek konteynerde ise servlet 500 üretir. Test, istisnanın SESSİZCE
         * yutulup 2xx dönmediğini kanıtlar.
         */
        @Test
        @DisplayName("I20: POST /refund-request - Başkasının ödemesi için talep 2xx ÜRETMEZ, istisna dışarı çıkar (IDOR)")
        void requestRefund_WhenPaymentBelongsToAnotherCustomer_ShouldNotSucceed() throws Exception {
            doThrow(new InvalidRefundException("You can only request a refund for your own payments."))
                    .when(commandUseCase).requestRefund(any(), any(), any());

            assertThatThrownBy(() -> mockMvc.perform(post("/api/payments/{id}/refund-request", UUID.randomUUID())
                    .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.customerToken(UUID.randomUUID())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\":\"x\"}")))
                    .hasRootCauseInstanceOf(InvalidRefundException.class);
        }

        @Test
        @DisplayName("I21: POST /refund-request - Ödeme bulunamazsa istek başarıyla sonuçlanmaz")
        void requestRefund_WhenPaymentDoesNotExist_ShouldNotSucceed() throws Exception {
            doThrow(new PaymentNotFoundException("Payment not found"))
                    .when(commandUseCase).requestRefund(any(), any(), any());

            assertThatThrownBy(() -> mockMvc.perform(post("/api/payments/{id}/refund-request", UUID.randomUUID())
                    .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.customerToken(UUID.randomUUID())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\":\"x\"}")))
                    .hasRootCauseInstanceOf(PaymentNotFoundException.class);
        }
    }

    // ==================== PATCH /{id}/refund-approve ====================

    @Nested
    @DisplayName("PATCH /api/payments/{id}/refund-approve - iade onayı")
    class RefundApproveEndpoint {

        @Test
        @DisplayName("I22: PATCH /refund-approve - ADMIN iadeyi onaylar, 200 döner")
        void approveRefund_WhenAdmin_ShouldReturn200() throws Exception {
            UUID paymentId = UUID.randomUUID();

            mockMvc.perform(patch("/api/payments/{id}/refund-approve", paymentId)
                            .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.adminToken(UUID.randomUUID()))))
                    .andExpect(status().isOk());

            verify(commandUseCase).approveRefund(paymentId);
        }

        @Test
        @DisplayName("I23: PATCH /refund-approve - CUSTOMER kendi iadesini onaylayamaz (yetki yükseltme engellenir)")
        void approveRefund_WhenCustomer_ShouldBeForbidden() throws Exception {
            mockMvc.perform(patch("/api/payments/{id}/refund-approve", UUID.randomUUID())
                            .header("Authorization", JwtTestTokens.bearer(
                                    JwtTestTokens.customerToken(UUID.randomUUID()))))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(commandUseCase);
        }

        @Test
        @DisplayName("I24: PATCH /refund-approve - STORE rolü de onaylayamaz")
        void approveRefund_WhenStore_ShouldBeForbidden() throws Exception {
            mockMvc.perform(patch("/api/payments/{id}/refund-approve", UUID.randomUUID())
                            .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.storeToken(UUID.randomUUID()))))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(commandUseCase);
        }

        @Test
        @DisplayName("I25: PATCH /refund-approve - Kimliksiz istek reddedilir")
        void approveRefund_WhenAnonymous_ShouldBeForbidden() throws Exception {
            mockMvc.perform(patch("/api/payments/{id}/refund-approve", UUID.randomUUID()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("I26: PATCH /refund-approve - Zaten iade edilmiş ödeme için istek 2xx ÜRETMEZ (çift iade)")
        void approveRefund_WhenAlreadyRefunded_ShouldNotSucceed() throws Exception {
            doThrow(new InvalidRefundException("Only payments with a pending refund request can be approved."))
                    .when(commandUseCase).approveRefund(any());

            assertThatThrownBy(() -> mockMvc.perform(patch("/api/payments/{id}/refund-approve", UUID.randomUUID())
                    .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.adminToken(UUID.randomUUID())))))
                    .hasRootCauseInstanceOf(InvalidRefundException.class);
        }
    }

    // ==================== /error erişilebilirliği ====================

    /**
     * REGRESYON (BUG DÜZELTMESİ): SecurityConfig'te {@code /error} permitAll DEĞİLDİ.
     * Servlet konteyneri 5xx'i ERROR dispatch ile /error'a yönlendirir; JwtAuthFilter
     * bir OncePerRequestFilter olduğu için o dispatch'te YENİDEN ÇALIŞMAZ ve
     * SecurityContext boş olur. Sonuç: gerçek 500'ler istemciye 403 olarak dönüyor,
     * hata gövdesi kayboluyordu.
     */
    @Test
    @DisplayName("I27: /error - Güvenlik kuralı tarafından engellenmemelidir (5xx'in 403'e dönüşmesi bug'ı)")
    void errorDispatchPath_ShouldNotBeBlockedBySecurityRules() throws Exception {
        int statusCode = mockMvc.perform(get("/error"))
                .andReturn().getResponse().getStatus();

        assertThat(statusCode)
                .as("/error permitAll değilse hata sayfası 403'e dönüşür ve gerçek hata gizlenir")
                .isNotEqualTo(403);
    }
}
