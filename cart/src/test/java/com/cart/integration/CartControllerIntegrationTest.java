package com.cart.integration;

import com.cart.api.controller.CartController;
import com.cart.api.dto.CartItemResponse;
import com.cart.api.dto.CartResponse;
import com.cart.application.port.in.CartCommandUseCase;
import com.cart.application.port.in.CartQueryUseCase;
import com.cart.domain.exception.UnauthorizedCartAccessException;
import com.cart.infrastructure.security.JwtAuthenticationProvider;
import com.cart.infrastructure.security.config.SecurityConfig;
import com.cart.infrastructure.security.filter.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
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

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Katman: INTEGRATION (@WebMvcTest slice + GERÇEK Spring Security filtre
 * zinciri, addFilters = true).
 *
 * Burada altyapı (Postgres/Redis/RabbitMQ) YOKTUR; use case'ler @MockitoBean
 * ile taklit edilir. Doğrulanan şey HTTP sözleşmesidir: status kodları,
 * Bean Validation, JWT ile kimlik doğrulama, rol tabanlı yetkilendirme ve
 * IDOR koruması.
 *
 * @WebMvcTest kullanıcının SecurityConfig'ini otomatik yüklemediği için
 * SecurityConfig + JwtAuthenticationFilter + JwtAuthenticationProvider açıkça
 * import edilir; böylece gerçek 401/403 davranışı gözlemlenebilir
 * (order servisindeki controller testinden farkı budur: orada addFilters=false
 * olduğu için yetki ihlalleri sadece exception olarak görülebiliyordu).
 */
@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationProvider.class})
@TestPropertySource(properties = "jwt.secret=" + CartControllerIntegrationTest.SECRET)
@DisplayName("INTEGRATION - CartController HTTP sözleşmesi (gerçek güvenlik filtre zinciri)")
class CartControllerIntegrationTest {

    static final String SECRET = "EcommerceSharedJwtSecretKey2026VeryLongAndSecure32Bytes";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private CartCommandUseCase cartCommandUseCase;

    @MockitoBean
    private CartQueryUseCase cartQueryUseCase;

    private UUID userId;
    private UUID otherUserId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    // --- yardımcılar -------------------------------------------------------

    private static String jwt(String subject, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .claims(Map.of("role", role))
                .issuedAt(new Date(now - 1000))
                .expiration(new Date(now + 600_000))
                .signWith(key)
                .compact();
    }

    private static String bearer(UUID subject, String role) {
        return "Bearer " + jwt(subject.toString(), role);
    }

    private String addBody(UUID productId, Integer quantity, String price) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "productId", productId,
                "quantity", quantity,
                "price", new BigDecimal(price)));
    }

    private CartResponse sampleResponse() {
        CartResponse response = new CartResponse();
        response.setUserId(userId);
        response.setTotalAmount(new BigDecimal("30.00"));
        response.setItems(List.of(new CartItemResponse(productId, 3, new BigDecimal("10.00"))));
        return response;
    }

    // --- kimlik doğrulama / yetkilendirme ----------------------------------

    /**
     * NOT (gerçek sözleşme 403, 401 DEĞİL): SecurityConfig'te bir
     * authenticationEntryPoint tanımlı olmadığı için Spring Security varsayılan
     * olarak Http403ForbiddenEntryPoint kullanır; kimliği doğrulanmamış istek
     * de ExceptionTranslationFilter tarafından 403 ile karşılanır. Semantik
     * olarak 401 daha doğru olurdu, ancak bu client-görünür bir davranış
     * değişikliği olur ve order/review servisleriyle tutarsızlık yaratır -
     * bu yüzden test mevcut gerçek davranışa sabitlenmiştir.
     */
    @Test
    @DisplayName("I1: GET /api/carts/{userId} - Token yoksa istek filtre zincirinde 403 ile reddedilir (varsayılan entry point)")
    void getCart_WhenNoToken_ShouldBeRejectedByFilterChain() throws Exception {
        mockMvc.perform(get("/api/carts/{userId}", userId))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cartQueryUseCase);
    }

    @Test
    @DisplayName("I2: GET /api/carts/{userId} - Geçersiz imzalı token ile erişim reddedilir")
    void getCart_WhenTokenSignatureInvalid_ShouldBeRejected() throws Exception {
        String foreignToken = Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of("role", "ROLE_CUSTOMER"))
                .expiration(new Date(System.currentTimeMillis() + 600_000))
                .signWith(Keys.hmacShaKeyFor("AnotherSecretKeyThatIsAlsoLongEnough2026ForHmacSha".getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/carts/{userId}", userId)
                        .header("Authorization", "Bearer " + foreignToken))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(cartQueryUseCase);
    }

    @Test
    @DisplayName("I3: GET /api/carts/{userId} - CUSTOMER/ADMIN dışı bir rol 403 Forbidden alır")
    void getCart_WhenRoleNotAllowed_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/carts/{userId}", userId)
                        .header("Authorization", bearer(userId, "ROLE_STORE")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(cartQueryUseCase);
    }

    @Test
    @DisplayName("I4: GET /api/carts/{userId} - Sepet sahibi CUSTOMER kendi sepetini 200 ile okur")
    void getCart_WhenOwner_ShouldReturn200WithCartBody() throws Exception {
        when(cartQueryUseCase.getCart(userId)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/carts/{userId}", userId)
                        .header("Authorization", bearer(userId, "ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.totalAmount").value(30.00))
                .andExpect(jsonPath("$.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(3));
    }

    @Test
    @DisplayName("I5: GET /api/carts/{userId} - Başkasının sepetini isteyen CUSTOMER IDOR koruması ile durdurulur")
    void getCart_WhenRequestingAnotherUsersCart_ShouldBeBlockedByIdorGuard() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/carts/{userId}", otherUserId)
                .header("Authorization", bearer(userId, "ROLE_CUSTOMER"))))
                .hasCauseInstanceOf(UnauthorizedCartAccessException.class);

        verifyNoInteractions(cartQueryUseCase);
    }

    @Test
    @DisplayName("I6: GET /api/carts/{userId} - ADMIN başkasının sepetini de okuyabilir")
    void getCart_WhenAdminRequestsAnotherUsersCart_ShouldReturn200() throws Exception {
        when(cartQueryUseCase.getCart(otherUserId)).thenReturn(new CartResponse());

        mockMvc.perform(get("/api/carts/{userId}", otherUserId)
                        .header("Authorization", bearer(userId, "ROLE_ADMIN")))
                .andExpect(status().isOk());

        verify(cartQueryUseCase).getCart(otherUserId);
    }

    // --- addItem sözleşmesi -------------------------------------------------

    @Test
    @DisplayName("I7: POST /{userId}/items - Geçerli istek 200 döner ve use case çağrılır")
    void addItem_WhenRequestValid_ShouldReturn200AndDelegate() throws Exception {
        mockMvc.perform(post("/api/carts/{userId}/items", userId)
                        .header("Authorization", bearer(userId, "ROLE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(productId, 2, "10.00")))
                .andExpect(status().isOk());

        verify(cartCommandUseCase).addItemToCart(eq(userId), any());
    }

    @Test
    @DisplayName("I8: POST /{userId}/items - quantity alanı hiç gönderilmezse 400 döner (500 DEĞİL)")
    void addItem_WhenQuantityMissing_ShouldReturn400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "productId", productId,
                "price", new BigDecimal("10.00")));

        mockMvc.perform(post("/api/carts/{userId}/items", userId)
                        .header("Authorization", bearer(userId, "ROLE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(cartCommandUseCase, never()).addItemToCart(any(), any());
    }

    @Test
    @DisplayName("I9: POST /{userId}/items - quantity 0 veya negatifse 400 döner (@Min(1))")
    void addItem_WhenQuantityNotPositive_ShouldReturn400() throws Exception {
        for (int quantity : new int[]{0, -5}) {
            mockMvc.perform(post("/api/carts/{userId}/items", userId)
                            .header("Authorization", bearer(userId, "ROLE_CUSTOMER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addBody(productId, quantity, "10.00")))
                    .andExpect(status().isBadRequest());
        }

        verify(cartCommandUseCase, never()).addItemToCart(any(), any());
    }

    @Test
    @DisplayName("I10: POST /{userId}/items - productId veya price null ise 400 döner")
    void addItem_WhenMandatoryFieldsMissing_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/carts/{userId}/items", userId)
                        .header("Authorization", bearer(userId, "ROLE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2,\"price\":10.00}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/carts/{userId}/items", userId)
                        .header("Authorization", bearer(userId, "ROLE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + productId + "\",\"quantity\":2}"))
                .andExpect(status().isBadRequest());

        verify(cartCommandUseCase, never()).addItemToCart(any(), any());
    }

    @Test
    @DisplayName("I11: POST /{userId}/items - Bozuk JSON gövdesi 400 döner")
    void addItem_WhenBodyIsMalformedJson_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/carts/{userId}/items", userId)
                        .header("Authorization", bearer(userId, "ROLE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("I12: POST /{userId}/items - Path'teki userId geçerli bir UUID değilse 400 döner")
    void addItem_WhenPathUserIdIsNotUuid_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/carts/{userId}/items", "not-a-uuid")
                        .header("Authorization", bearer(userId, "ROLE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(productId, 1, "10.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("I13: POST /{userId}/items - Başkasının sepetine ekleme IDOR koruması ile durdurulur")
    void addItem_WhenTargetingAnotherUsersCart_ShouldBeBlockedByIdorGuard() {
        assertThatThrownBy(() -> mockMvc.perform(post("/api/carts/{userId}/items", otherUserId)
                .header("Authorization", bearer(userId, "ROLE_CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addBody(productId, 1, "10.00"))))
                .hasCauseInstanceOf(UnauthorizedCartAccessException.class);

        verifyNoInteractions(cartCommandUseCase);
    }

    // --- updateItem / removeItem / clearCart --------------------------------

    @Test
    @DisplayName("I14: PUT /{userId}/items/{productId} - quantity 0 geçerlidir (satır silme anlamına gelir) ve 200 döner")
    void updateItem_WhenQuantityZero_ShouldReturn200BecauseZeroMeansRemoval() throws Exception {
        mockMvc.perform(put("/api/carts/{userId}/items/{productId}", userId, productId)
                        .header("Authorization", bearer(userId, "ROLE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isOk());

        verify(cartCommandUseCase).updateCartItemQuantity(eq(userId), eq(productId), any());
    }

    @Test
    @DisplayName("I15: PUT /{userId}/items/{productId} - Negatif quantity 400 döner (@Min(0))")
    void updateItem_WhenQuantityNegative_ShouldReturn400() throws Exception {
        mockMvc.perform(put("/api/carts/{userId}/items/{productId}", userId, productId)
                        .header("Authorization", bearer(userId, "ROLE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":-1}"))
                .andExpect(status().isBadRequest());

        verify(cartCommandUseCase, never()).updateCartItemQuantity(any(), any(), any());
    }

    @Test
    @DisplayName("I16: PUT /{userId}/items/{productId} - quantity null ise 400 döner")
    void updateItem_WhenQuantityNull_ShouldReturn400() throws Exception {
        mockMvc.perform(put("/api/carts/{userId}/items/{productId}", userId, productId)
                        .header("Authorization", bearer(userId, "ROLE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("I17: DELETE /{userId}/items/{productId} - Sahibi satırı siler ve 200 alır")
    void removeItem_WhenOwner_ShouldReturn200AndDelegate() throws Exception {
        mockMvc.perform(delete("/api/carts/{userId}/items/{productId}", userId, productId)
                        .header("Authorization", bearer(userId, "ROLE_CUSTOMER")))
                .andExpect(status().isOk());

        verify(cartCommandUseCase).removeCartItem(userId, productId);
    }

    @Test
    @DisplayName("I18: DELETE /{userId} - Sahibi sepeti tamamen boşaltır ve 200 alır")
    void clearCart_WhenOwner_ShouldReturn200AndDelegate() throws Exception {
        mockMvc.perform(delete("/api/carts/{userId}", userId)
                        .header("Authorization", bearer(userId, "ROLE_CUSTOMER")))
                .andExpect(status().isOk());

        verify(cartCommandUseCase).clearCart(userId);
    }

    @Test
    @DisplayName("I19: DELETE /{userId} - Başkasının sepetini boşaltma IDOR koruması ile durdurulur")
    void clearCart_WhenTargetingAnotherUsersCart_ShouldBeBlockedByIdorGuard() {
        assertThatThrownBy(() -> mockMvc.perform(delete("/api/carts/{userId}", otherUserId)
                .header("Authorization", bearer(userId, "ROLE_CUSTOMER"))))
                .hasCauseInstanceOf(UnauthorizedCartAccessException.class);

        verifyNoInteractions(cartCommandUseCase);
    }

    @Test
    @DisplayName("I20: DELETE /{userId}/items/{productId} - Süresi dolmuş token ile erişim reddedilir")
    void removeItem_WhenTokenExpired_ShouldBeRejected() throws Exception {
        String expired = Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of("role", "ROLE_CUSTOMER"))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(delete("/api/carts/{userId}/items/{productId}", userId, productId)
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(cartCommandUseCase);
    }
}
