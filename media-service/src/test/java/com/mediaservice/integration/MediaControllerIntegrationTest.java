package com.mediaservice.integration;

import com.mediaservice.api.controller.MediaController;
import com.mediaservice.application.port.in.MediaCommandUseCase;
import com.mediaservice.application.port.in.MediaQueryUseCase;
import com.mediaservice.domain.exception.MediaAssetNotFoundException;
import com.mediaservice.domain.exception.UnauthorizedMediaAccessException;
import com.mediaservice.domain.model.MediaAsset;
import com.mediaservice.infrastructure.security.JwtAuthFilter;
import com.mediaservice.infrastructure.config.SecurityConfig;
import com.mediaservice.support.JwtTestTokens;
import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Katman: INTEGRATION (slice) - GERCEK Spring Security filtre zinciri (addFilters=true)
 * + gercek {@link JwtAuthFilter} / {@link SecurityConfig} ile MediaController'in HTTP
 * sozlesmesi dogrulanir. Token'lar gercekten imzalanir. Use case'ler {@code @MockBean}
 * ile izole edilir: burada test edilen is mantigi degil, durum kodlari / rol bazli
 * yetkilendirme / validasyon / hata semasidir.
 */
@WebMvcTest(MediaController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, JwtAuthFilter.class})
@TestPropertySource(properties = "app.security.jwt-secret=" + JwtTestTokens.SECRET)
@DisplayName("Media Service - Integration: MediaController (gercek guvenlik filtre zinciri)")
class MediaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaCommandUseCase commandUseCase;

    @MockBean
    private MediaQueryUseCase queryUseCase;

    // ==================== POST /products/{productId}/images (ROLE_STORE) ====================

    @Test
    @DisplayName("I1: Upload - ROLE_STORE token ile 201 doner")
    void uploadProductImage_WhenStoreRole_ShouldReturn201() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        MediaAsset asset = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, true);
        when(commandUseCase.uploadProductImage(eq(productId), any(), anyBoolean(), anyString(), any()))
                .thenReturn(asset);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/media/products/{productId}/images", productId)
                        .file(file)
                        .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.storeToken(storeId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetId").value(asset.getId().toString()));
    }

    @Test
    @DisplayName("I2: Upload - ROLE_CUSTOMER token ile 403 (yetkisiz rol)")
    void uploadProductImage_WhenCustomerRole_ShouldReturn403() throws Exception {
        UUID productId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/media/products/{productId}/images", productId)
                        .file(file)
                        .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.customerToken(UUID.randomUUID()))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I3: Upload - Token yoksa 401/403 (kimliksiz istek reddedilir), use case HIC cagrilmaz")
    void uploadProductImage_WhenNoToken_ShouldBeRejected() throws Exception {
        UUID productId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/media/products/{productId}/images", productId).file(file))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
        org.mockito.Mockito.verifyNoInteractions(commandUseCase);
    }

    // ==================== DELETE /images/{assetId} (STORE veya ADMIN) ====================

    @Test
    @DisplayName("I4: Delete - ROLE_ADMIN gorseli silebilir (204)")
    void deleteProductImage_WhenAdmin_ShouldReturn204() throws Exception {
        UUID assetId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/media/images/{assetId}", assetId)
                        .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.adminToken(UUID.randomUUID()))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("I5: Delete - Use case UnauthorizedMediaAccessException firlatirsa 403 govdesi doner")
    void deleteProductImage_WhenUseCaseThrowsUnauthorized_ShouldReturn403WithErrorBody() throws Exception {
        UUID assetId = UUID.randomUUID();
        doThrow(new UnauthorizedMediaAccessException("Bu gorsel uzerinde islem yetkiniz yok."))
                .when(commandUseCase).deleteProductImage(eq(assetId), any(), anyBoolean());

        mockMvc.perform(delete("/api/v1/media/images/{assetId}", assetId)
                        .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.storeToken(UUID.randomUUID()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("I6: Delete - Asset bulunamazsa 404 govdesi doner")
    void deleteProductImage_WhenNotFound_ShouldReturn404() throws Exception {
        UUID assetId = UUID.randomUUID();
        doThrow(new MediaAssetNotFoundException(assetId))
                .when(commandUseCase).deleteProductImage(eq(assetId), any(), anyBoolean());

        mockMvc.perform(delete("/api/v1/media/images/{assetId}", assetId)
                        .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.storeToken(UUID.randomUUID()))))
                .andExpect(status().isNotFound());
    }

    // ==================== PUT /products/{productId}/images/order ====================

    @Test
    @DisplayName("I7: Reorder - Bos assetIds listesi @Valid tarafindan 400 ile reddedilir")
    void reorderProductImages_WhenAssetIdsEmpty_ShouldReturn400() throws Exception {
        UUID productId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/media/products/{productId}/images/order", productId)
                        .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.storeToken(UUID.randomUUID())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetIds\":[]}"))
                .andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(commandUseCase);
    }

    @Test
    @DisplayName("I8: Reorder - Gecerli istek ve ROLE_STORE ile 200 doner")
    void reorderProductImages_WhenValid_ShouldReturn200() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        when(commandUseCase.reorderProductImages(eq(productId), any(), any(), anyBoolean()))
                .thenReturn(List.of());

        mockMvc.perform(put("/api/v1/media/products/{productId}/images/order", productId)
                        .header("Authorization", JwtTestTokens.bearer(JwtTestTokens.storeToken(UUID.randomUUID())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetIds\":[\"" + assetId + "\"]}"))
                .andExpect(status().isOk());
    }

    // ==================== GET /products/{productId}/images (public) ====================

    @Test
    @DisplayName("I9: getProductMedia - Public endpoint, token GEREKMEZ")
    void getProductMedia_ShouldBePublic() throws Exception {
        UUID productId = UUID.randomUUID();
        when(queryUseCase.getProductMedia(productId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/media/products/{productId}/images", productId))
                .andExpect(status().isOk());
    }

    // ==================== POST /products/images/batch (public + validasyon) ====================

    @Test
    @DisplayName("I10: Batch - 100'den fazla id icin @Valid 400 ile reddeder (token gerekmeden)")
    void getProductMediaBatch_WhenOverLimit_ShouldReturn400() throws Exception {
        StringBuilder ids = new StringBuilder("[");
        for (int i = 0; i < 101; i++) {
            if (i > 0) ids.append(",");
            ids.append("\"").append(UUID.randomUUID()).append("\"");
        }
        ids.append("]");

        mockMvc.perform(post("/api/v1/media/products/images/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productIds\":" + ids + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("I11: Batch - Gecerli istek 200 doner, hicbir authorization header gerekmez")
    void getProductMediaBatch_WhenValid_ShouldReturn200WithoutAuth() throws Exception {
        UUID productId = UUID.randomUUID();
        when(queryUseCase.getProductMediaBatch(any())).thenReturn(java.util.Map.of());

        mockMvc.perform(post("/api/v1/media/products/images/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productIds\":[\"" + productId + "\"]}"))
                .andExpect(status().isOk());
    }
}
