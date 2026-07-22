package com.review.integration;

import com.review.api.controller.ReviewController;
import com.review.api.dto.ReviewResponse;
import com.review.application.port.in.ReviewCommandUseCase;
import com.review.application.port.in.ReviewQueryUseCase;
import com.review.domain.model.ReviewStatus;
import com.review.infrastructure.security.JwtAuthFilter;
import com.review.infrastructure.security.JwtTokenProvider;
import com.review.infrastructure.security.SecurityConfig;
import com.review.unit.support.JwtTestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration (slice) katmanı: GERÇEK Spring Security filtre zinciri
 * (addFilters = true) + gerçek JwtAuthFilter/JwtTokenProvider + gerçek
 * SecurityConfig ile ReviewController'ın HTTP sözleşmesi doğrulanır.
 * Use case'ler @MockitoBean ile izole edilir - burada test edilen şey iş
 * mantığı değil, HTTP durum kodları / validasyon / yetkilendirmedir.
 *
 * NOT: Uygulamada özel bir AuthenticationEntryPoint tanımlı olmadığı için
 * Spring Security'nin varsayılanı devrededir; kimliksiz istek de yetkisiz
 * istek de 403 üretir. Testler gözlemlenen gerçek davranışı sabitler.
 */
@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtTokenProvider.class})
@TestPropertySource(properties = "jwt.secret=" + JwtTestTokens.SECRET)
@DisplayName("Review Service - Integration: ReviewController (gerçek güvenlik filtre zinciri)")
class ReviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewCommandUseCase commandUseCase;

    @MockitoBean
    private ReviewQueryUseCase queryUseCase;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static ReviewResponse sampleResponse(String id, ReviewStatus status) {
        ReviewResponse response = new ReviewResponse();
        response.setId(id);
        response.setProductId("prod-1");
        response.setCustomerId("cust-1");
        response.setRating(5);
        response.setComment("harika");
        response.setStatus(status.name());
        response.setCreatedAt(LocalDateTime.of(2026, 1, 15, 10, 30));
        return response;
    }

    // ---------- GET /api/reviews/product/{productId} : herkese açık ----------

    @Test
    @DisplayName("I1: GET /product/{id} - Token olmadan 200 döner, ortalama puan ve yorum listesi içerir")
    void getProductReviews_WhenAnonymous_ShouldReturn200WithAverageAndReviews() throws Exception {
        when(queryUseCase.getProductReviews("prod-1")).thenReturn(List.of(sampleResponse("r-1", ReviewStatus.ACTIVE)));
        when(queryUseCase.getProductAverageRating("prod-1")).thenReturn(4.5);

        mockMvc.perform(get("/api/reviews/product/prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.reviews[0].id").value("r-1"))
                .andExpect(jsonPath("$.reviews[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("I2: GET /product/{id} - LocalDateTime alanları ISO-8601 string olarak serialize edilir")
    void getProductReviews_ShouldSerializeLocalDateTimeAsIsoString() throws Exception {
        when(queryUseCase.getProductReviews("prod-1")).thenReturn(List.of(sampleResponse("r-1", ReviewStatus.ACTIVE)));
        when(queryUseCase.getProductAverageRating("prod-1")).thenReturn(5.0);

        mockMvc.perform(get("/api/reviews/product/prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews[0].createdAt").value("2026-01-15T10:30:00"));
    }

    @Test
    @DisplayName("I3: GET /product/{id} - Hiç yorum yoksa boş liste ve 0.0 ortalama döner")
    void getProductReviews_WhenNoReviews_ShouldReturnEmptyListAndZeroAverage() throws Exception {
        when(queryUseCase.getProductReviews("prod-x")).thenReturn(List.of());
        when(queryUseCase.getProductAverageRating("prod-x")).thenReturn(0.0);

        mockMvc.perform(get("/api/reviews/product/prod-x"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.reviews").isEmpty());
    }

    // ---------- POST /api/reviews : ROLE_CUSTOMER ----------

    @Test
    @DisplayName("I4: POST /api/reviews - CUSTOMER rolü yorum oluşturabilir, gövdede reviewId döner")
    void createReview_WhenCustomer_ShouldReturn200WithReviewId() throws Exception {
        when(commandUseCase.createReview(eq("cust-1"), any())).thenReturn("review-99");

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(JwtTestTokens.customerToken("cust-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"order-1\",\"productId\":\"prod-1\",\"rating\":5,\"comment\":\"harika\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("review-99"));
    }

    @Test
    @DisplayName("I5: POST /api/reviews - customerId gövdeden değil JWT subject'inden alınır (IDOR koruması)")
    void createReview_ShouldTakeCustomerIdFromJwtSubjectNotFromBody() throws Exception {
        when(commandUseCase.createReview(anyString(), any())).thenReturn("review-99");

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(JwtTestTokens.customerToken("cust-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"order-1\",\"productId\":\"prod-1\",\"rating\":5,"
                                + "\"customerId\":\"victim-user\"}"))
                .andExpect(status().isOk());

        verify(commandUseCase).createReview(eq("cust-1"), any());
        verify(commandUseCase, never()).createReview(eq("victim-user"), any());
    }

    @Test
    @DisplayName("I6: POST /api/reviews - Token olmadan erişim reddedilir ve use case hiç çağrılmaz")
    void createReview_WhenNoToken_ShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"order-1\",\"productId\":\"prod-1\",\"rating\":5}"))
                .andExpect(status().isForbidden());

        verify(commandUseCase, never()).createReview(anyString(), any());
    }

    @Test
    @DisplayName("I7: POST /api/reviews - STORE rolü yorum oluşturamaz (403)")
    void createReview_WhenStoreRole_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(JwtTestTokens.storeToken("store-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"order-1\",\"productId\":\"prod-1\",\"rating\":5}"))
                .andExpect(status().isForbidden());

        verify(commandUseCase, never()).createReview(anyString(), any());
    }

    @Test
    @DisplayName("I8: POST /api/reviews - Geçersiz imzalı token 403 ile reddedilir")
    void createReview_WithTamperedToken_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization",
                                bearer(JwtTestTokens.tokenSignedWithWrongSecret("cust-1", "CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"order-1\",\"productId\":\"prod-1\",\"rating\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I9: POST /api/reviews - rating 5'ten büyükse 400 Bad Request")
    void createReview_WithRatingAboveMax_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(JwtTestTokens.customerToken("cust-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"order-1\",\"productId\":\"prod-1\",\"rating\":6}"))
                .andExpect(status().isBadRequest());

        verify(commandUseCase, never()).createReview(anyString(), any());
    }

    @Test
    @DisplayName("I10: POST /api/reviews - rating 1'den küçükse 400 Bad Request")
    void createReview_WithRatingBelowMin_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(JwtTestTokens.customerToken("cust-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"order-1\",\"productId\":\"prod-1\",\"rating\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("I11: POST /api/reviews - rating hiç gönderilmezse 400 Bad Request")
    void createReview_WithoutRating_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(JwtTestTokens.customerToken("cust-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"order-1\",\"productId\":\"prod-1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("I12: POST /api/reviews - orderId boşsa 400 Bad Request")
    void createReview_WithBlankOrderId_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(JwtTestTokens.customerToken("cust-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"  \",\"productId\":\"prod-1\",\"rating\":5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("I13: POST /api/reviews - productId boşsa 400 Bad Request")
    void createReview_WithBlankProductId_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(JwtTestTokens.customerToken("cust-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"order-1\",\"productId\":\"\",\"rating\":5}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /api/reviews/me : ROLE_CUSTOMER ----------

    @Test
    @DisplayName("I14: GET /me - CUSTOMER kendi yorumlarını görebilir")
    void getMyReviews_WhenCustomer_ShouldReturn200() throws Exception {
        when(queryUseCase.getMyReviews("cust-1")).thenReturn(List.of(sampleResponse("r-1", ReviewStatus.ACTIVE)));

        mockMvc.perform(get("/api/reviews/me")
                        .header("Authorization", bearer(JwtTestTokens.customerToken("cust-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("r-1"));

        verify(queryUseCase).getMyReviews("cust-1");
    }

    @Test
    @DisplayName("I15: GET /me - ADMIN rolü bu endpoint'e erişemez (yalnızca CUSTOMER)")
    void getMyReviews_WhenAdmin_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/reviews/me")
                        .header("Authorization", bearer(JwtTestTokens.adminToken("admin-1"))))
                .andExpect(status().isForbidden());

        verify(queryUseCase, never()).getMyReviews(anyString());
    }

    @Test
    @DisplayName("I16: GET /me - Token olmadan erişim reddedilir")
    void getMyReviews_WhenAnonymous_ShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/reviews/me"))
                .andExpect(status().isForbidden());
    }

    // ---------- PATCH /api/reviews/{id}/reply : ROLE_STORE ----------

    @Test
    @DisplayName("I17: PATCH /{id}/reply - STORE rolü yoruma mağaza cevabı yazabilir")
    void replyToReview_WhenStore_ShouldReturn200() throws Exception {
        mockMvc.perform(patch("/api/reviews/r-1/reply")
                        .header("Authorization", bearer(JwtTestTokens.storeToken("store-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"replyText\":\"Teşekkür ederiz\"}"))
                .andExpect(status().isOk());

        verify(commandUseCase).replyToReview(eq("r-1"), any());
    }

    @Test
    @DisplayName("I18: PATCH /{id}/reply - CUSTOMER rolü mağaza cevabı yazamaz (403)")
    void replyToReview_WhenCustomer_ShouldReturn403() throws Exception {
        mockMvc.perform(patch("/api/reviews/r-1/reply")
                        .header("Authorization", bearer(JwtTestTokens.customerToken("cust-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"replyText\":\"kendi yorumuma cevap\"}"))
                .andExpect(status().isForbidden());

        verify(commandUseCase, never()).replyToReview(anyString(), any());
    }

    @Test
    @DisplayName("I19: PATCH /{id}/reply - ADMIN rolü de mağaza cevabı yazamaz (yetki ayrımı)")
    void replyToReview_WhenAdmin_ShouldReturn403() throws Exception {
        mockMvc.perform(patch("/api/reviews/r-1/reply")
                        .header("Authorization", bearer(JwtTestTokens.adminToken("admin-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"replyText\":\"admin cevabı\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I20: PATCH /{id}/reply - Boş cevap metni 400 Bad Request üretir")
    void replyToReview_WithBlankReplyText_ShouldReturn400() throws Exception {
        mockMvc.perform(patch("/api/reviews/r-1/reply")
                        .header("Authorization", bearer(JwtTestTokens.storeToken("store-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"replyText\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verify(commandUseCase, never()).replyToReview(anyString(), any());
    }

    @Test
    @DisplayName("I21: PATCH /{id}/reply - Token olmadan erişim reddedilir")
    void replyToReview_WhenAnonymous_ShouldBeRejected() throws Exception {
        mockMvc.perform(patch("/api/reviews/r-1/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"replyText\":\"cevap\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------- PATCH /api/reviews/{id}/moderate : ROLE_ADMIN ----------

    @Test
    @DisplayName("I22: PATCH /{id}/moderate - ADMIN yorumu HIDDEN yapabilir")
    void moderateReview_WhenAdminHidesReview_ShouldReturn200() throws Exception {
        mockMvc.perform(patch("/api/reviews/r-1/moderate")
                        .header("Authorization", bearer(JwtTestTokens.adminToken("admin-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\"}"))
                .andExpect(status().isOk());

        verify(commandUseCase).moderateReview(eq("r-1"), any());
    }

    @Test
    @DisplayName("I23: PATCH /{id}/moderate - ADMIN gizlenmiş yorumu tekrar ACTIVE yapabilir")
    void moderateReview_WhenAdminReactivatesReview_ShouldReturn200() throws Exception {
        mockMvc.perform(patch("/api/reviews/r-1/moderate")
                        .header("Authorization", bearer(JwtTestTokens.adminToken("admin-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("I24: PATCH /{id}/moderate - CUSTOMER moderasyon yapamaz (403)")
    void moderateReview_WhenCustomer_ShouldReturn403() throws Exception {
        mockMvc.perform(patch("/api/reviews/r-1/moderate")
                        .header("Authorization", bearer(JwtTestTokens.customerToken("cust-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\"}"))
                .andExpect(status().isForbidden());

        verify(commandUseCase, never()).moderateReview(anyString(), any());
    }

    @Test
    @DisplayName("I25: PATCH /{id}/moderate - STORE moderasyon yapamaz (403)")
    void moderateReview_WhenStore_ShouldReturn403() throws Exception {
        mockMvc.perform(patch("/api/reviews/r-1/moderate")
                        .header("Authorization", bearer(JwtTestTokens.storeToken("store-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I26: PATCH /{id}/moderate - Tanımsız durum değeri 400 Bad Request üretir")
    void moderateReview_WithUnknownStatus_ShouldReturn400() throws Exception {
        mockMvc.perform(patch("/api/reviews/r-1/moderate")
                        .header("Authorization", bearer(JwtTestTokens.adminToken("admin-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELETED\"}"))
                .andExpect(status().isBadRequest());

        verify(commandUseCase, never()).moderateReview(anyString(), any());
    }

    @Test
    @DisplayName("I27: PATCH /{id}/moderate - status null ise 400 döner (500 NPE değil) [BUGFIX regresyonu]")
    void moderateReview_WithNullStatus_ShouldReturn400NotServerError() throws Exception {
        // ModerateReviewRequest.status'e @NotBlank eklenmeden önce @Pattern null'ı
        // GEÇERLİ sayıyordu; istek use case'e ulaşıp ReviewStatus.valueOf(null)
        // ile NPE -> HTTP 500 üretiyordu.
        mockMvc.perform(patch("/api/reviews/r-1/moderate")
                        .header("Authorization", bearer(JwtTestTokens.adminToken("admin-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":null}"))
                .andExpect(status().isBadRequest());

        verify(commandUseCase, never()).moderateReview(anyString(), any());
    }

    @Test
    @DisplayName("I28: PATCH /{id}/moderate - status alanı hiç gönderilmezse 400 döner [BUGFIX regresyonu]")
    void moderateReview_WithMissingStatusField_ShouldReturn400() throws Exception {
        mockMvc.perform(patch("/api/reviews/r-1/moderate")
                        .header("Authorization", bearer(JwtTestTokens.adminToken("admin-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(commandUseCase, never()).moderateReview(anyString(), any());
    }

    // ---------- GET /api/reviews/all : ROLE_ADMIN ----------

    @Test
    @DisplayName("I29: GET /all - ADMIN tüm yorumları (HIDDEN dahil) görebilir")
    void getAllReviews_WhenAdmin_ShouldReturn200WithEveryReview() throws Exception {
        when(queryUseCase.getAllReviews()).thenReturn(List.of(
                sampleResponse("r-1", ReviewStatus.ACTIVE),
                sampleResponse("r-2", ReviewStatus.HIDDEN)));

        mockMvc.perform(get("/api/reviews/all")
                        .header("Authorization", bearer(JwtTestTokens.adminToken("admin-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].status").value("HIDDEN"));
    }

    @Test
    @DisplayName("I30: GET /all - CUSTOMER moderasyon listesine erişemez (403)")
    void getAllReviews_WhenCustomer_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/reviews/all")
                        .header("Authorization", bearer(JwtTestTokens.customerToken("cust-1"))))
                .andExpect(status().isForbidden());

        verify(queryUseCase, never()).getAllReviews();
    }

    @Test
    @DisplayName("I31: GET /all - Token olmadan erişim reddedilir")
    void getAllReviews_WhenAnonymous_ShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/reviews/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I32: GET /all - Rolsüz geçerli token da yetkisizdir (yetki kontrolü rol bazlı)")
    void getAllReviews_WithValidTokenWithoutRoles_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/reviews/all")
                        .header("Authorization", bearer(JwtTestTokens.tokenWithoutRoles("someone"))))
                .andExpect(status().isForbidden());
    }
}
