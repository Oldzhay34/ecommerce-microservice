package com.review.unit.usecase;

import com.review.api.dto.ReviewResponse;
import com.review.application.port.out.ReviewQueryPort;
import com.review.application.usecase.ReviewQueryUseCaseImpl;
import com.review.domain.model.Review;
import com.review.domain.model.ReviewStatus;
import com.review.unit.support.ReviewTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Review Service - Unit: ReviewQueryUseCaseImpl")
class ReviewQueryUseCaseImplTest {

    @Mock
    private ReviewQueryPort queryPort;

    @InjectMocks
    private ReviewQueryUseCaseImpl useCase;

    @Test
    @DisplayName("U16: getProductReviews - Yalnızca ACTIVE yorumları döner ve tüm alanları DTO'ya taşır")
    void getProductReviews_WhenActiveReviewsExist_ShouldMapAllFieldsToResponse() {
        Review review = ReviewTestFixtures.review("review-1", "prod-1", "cust-1", 5, ReviewStatus.ACTIVE);
        review.setStoreReplyText("teşekkürler");
        review.setStoreRepliedAt(LocalDateTime.of(2026, 1, 16, 9, 0));
        when(queryPort.findActiveByProductId("prod-1")).thenReturn(List.of(review));

        List<ReviewResponse> result = useCase.getProductReviews("prod-1");

        assertThat(result).hasSize(1);
        ReviewResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo("review-1");
        assertThat(response.getProductId()).isEqualTo("prod-1");
        assertThat(response.getCustomerId()).isEqualTo("cust-1");
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getStoreReplyText()).isEqualTo("teşekkürler");
        assertThat(response.getStoreRepliedAt()).isEqualTo(LocalDateTime.of(2026, 1, 16, 9, 0));
        assertThat(response.getCreatedAt()).isEqualTo(ReviewTestFixtures.FIXED_CREATED_AT);
    }

    @Test
    @DisplayName("U17: getProductReviews - Hiç yorum yoksa boş liste döner")
    void getProductReviews_WhenNoReviews_ShouldReturnEmptyList() {
        when(queryPort.findActiveByProductId("prod-x")).thenReturn(List.of());

        assertThat(useCase.getProductReviews("prod-x")).isEmpty();
    }

    @Test
    @DisplayName("U18: getMyReviews - Müşteriye ait yorumları (HIDDEN dahil) döner")
    void getMyReviews_WhenCustomerHasReviews_ShouldReturnAllOfThemIncludingHidden() {
        Review active = ReviewTestFixtures.review("r-1", "prod-1", "cust-1", 5, ReviewStatus.ACTIVE);
        Review hidden = ReviewTestFixtures.review("r-2", "prod-2", "cust-1", 1, ReviewStatus.HIDDEN);
        when(queryPort.findByCustomerId("cust-1")).thenReturn(List.of(active, hidden));

        List<ReviewResponse> result = useCase.getMyReviews("cust-1");

        assertThat(result).extracting(ReviewResponse::getStatus)
                .containsExactly("ACTIVE", "HIDDEN");
    }

    @Test
    @DisplayName("U19: getAllReviews - Moderasyon paneli için tüm yorumları döner")
    void getAllReviews_WhenReviewsExist_ShouldReturnAllReviews() {
        when(queryPort.findAll()).thenReturn(List.of(
                ReviewTestFixtures.review("r-1", "prod-1", "cust-1", 5, ReviewStatus.ACTIVE),
                ReviewTestFixtures.review("r-2", "prod-2", "cust-2", 2, ReviewStatus.HIDDEN)));

        assertThat(useCase.getAllReviews()).hasSize(2)
                .extracting(ReviewResponse::getId).containsExactly("r-1", "r-2");
    }

    @Test
    @DisplayName("U20: getProductAverageRating - Port'un hesapladığı ortalamayı olduğu gibi iletir")
    void getProductAverageRating_WhenReviewsExist_ShouldDelegateToPort() {
        when(queryPort.getAverageRatingForProduct("prod-1")).thenReturn(4.5);

        assertThat(useCase.getProductAverageRating("prod-1")).isEqualTo(4.5);
    }

    @Test
    @DisplayName("U21: getProductAverageRating - Yorum yoksa 0.0 döner")
    void getProductAverageRating_WhenNoReviews_ShouldReturnZero() {
        when(queryPort.getAverageRatingForProduct("prod-x")).thenReturn(0.0);

        assertThat(useCase.getProductAverageRating("prod-x")).isEqualTo(0.0);
    }
}
