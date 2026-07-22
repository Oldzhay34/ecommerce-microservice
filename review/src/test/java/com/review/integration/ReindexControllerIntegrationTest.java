package com.review.integration;

import com.review.api.controller.ReindexController;
import com.review.domain.model.Review;
import com.review.domain.model.ReviewStatus;
import com.review.infrastructure.persistence.entity.ReviewJpaEntity;
import com.review.infrastructure.persistence.mapper.ReviewEntityMapper;
import com.review.infrastructure.persistence.repository.ReviewRepository;
import com.review.infrastructure.search.adapter.ReviewSearchAdapter;
import com.review.infrastructure.security.JwtAuthFilter;
import com.review.infrastructure.security.JwtTokenProvider;
import com.review.infrastructure.security.SecurityConfig;
import com.review.unit.support.JwtTestTokens;
import com.review.unit.support.ReviewTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ReindexController, Postgres (write model) -> Elasticsearch (read model)
 * yeniden indeksleme uçtur. SecurityConfig'te /api/internal/** permitAll
 * olduğu için kimlik doğrulaması aranmaz; bu testler o sözleşmeyi ve
 * reindex'in HER kayda dokunduğunu sabitler.
 */
@WebMvcTest(ReindexController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtTokenProvider.class})
@TestPropertySource(properties = "jwt.secret=" + JwtTestTokens.SECRET)
@DisplayName("Review Service - Integration: ReindexController (Elasticsearch reindex)")
class ReindexControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private ReviewEntityMapper entityMapper;

    @MockitoBean
    private ReviewSearchAdapter searchAdapter;

    @Test
    @DisplayName("I33: POST /api/internal/reindex - Kimlik doğrulamasız erişilebilir ve indekslenen sayıyı döner")
    void reindex_WhenAnonymous_ShouldReturn200WithIndexedCount() throws Exception {
        ReviewJpaEntity e1 = ReviewTestFixtures.reviewEntity("r-1", "prod-1", "cust-1", 5, ReviewStatus.ACTIVE);
        ReviewJpaEntity e2 = ReviewTestFixtures.reviewEntity("r-2", "prod-2", "cust-2", 3, ReviewStatus.HIDDEN);
        when(reviewRepository.findAll()).thenReturn(List.of(e1, e2));
        when(entityMapper.toDomain(any(ReviewJpaEntity.class)))
                .thenReturn(ReviewTestFixtures.review("r-1", "prod-1", "cust-1", 5, ReviewStatus.ACTIVE));

        mockMvc.perform(post("/api/internal/reindex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexed").value(2));
    }

    @Test
    @DisplayName("I34: POST /api/internal/reindex - Postgres'teki HER kayıt için Elasticsearch senkronu çağrılır")
    void reindex_ShouldSyncEveryPersistedReviewToElasticsearch() throws Exception {
        when(reviewRepository.findAll()).thenReturn(List.of(
                ReviewTestFixtures.reviewEntity("r-1", "prod-1", "cust-1", 5, ReviewStatus.ACTIVE),
                ReviewTestFixtures.reviewEntity("r-2", "prod-2", "cust-2", 3, ReviewStatus.HIDDEN),
                ReviewTestFixtures.reviewEntity("r-3", "prod-3", "cust-3", 1, ReviewStatus.ACTIVE)));
        when(entityMapper.toDomain(any(ReviewJpaEntity.class)))
                .thenReturn(ReviewTestFixtures.review("r-1", "prod-1", "cust-1", 5, ReviewStatus.ACTIVE));

        mockMvc.perform(post("/api/internal/reindex")).andExpect(status().isOk());

        verify(entityMapper, times(3)).toDomain(any(ReviewJpaEntity.class));
        verify(searchAdapter, times(3)).syncToElasticsearch(any(Review.class));
    }

    @Test
    @DisplayName("I35: POST /api/internal/reindex - Hiç kayıt yoksa 0 döner ve ES'e dokunulmaz")
    void reindex_WhenNoReviewsExist_ShouldReturnZeroAndNotTouchElasticsearch() throws Exception {
        when(reviewRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(post("/api/internal/reindex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexed").value(0));

        verify(searchAdapter, never()).syncToElasticsearch(any(Review.class));
    }

    @Test
    @DisplayName("I36: GET /api/internal/reindex - Yalnızca POST desteklenir, GET 405 döner")
    void reindex_WhenCalledWithGet_ShouldReturn405() throws Exception {
        mockMvc.perform(get("/api/internal/reindex"))
                .andExpect(status().isMethodNotAllowed());
    }
}
