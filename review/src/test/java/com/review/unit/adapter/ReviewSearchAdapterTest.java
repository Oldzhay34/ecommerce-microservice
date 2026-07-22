package com.review.unit.adapter;

import com.review.domain.model.Review;
import com.review.domain.model.ReviewStatus;
import com.review.infrastructure.search.adapter.ReviewSearchAdapter;
import com.review.infrastructure.search.document.ReviewDocument;
import com.review.infrastructure.search.mapper.ReviewDocumentMapper;
import com.review.infrastructure.search.repository.ReviewSearchRepository;
import com.review.unit.support.ReviewTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Review Service - Unit: ReviewSearchAdapter (CQRS read side / Elasticsearch)")
class ReviewSearchAdapterTest {

    @Mock
    private ReviewSearchRepository repository;

    @Spy
    private ReviewDocumentMapper mapper = new ReviewDocumentMapper();

    private ReviewSearchAdapter adapter() {
        return new ReviewSearchAdapter(repository, mapper);
    }

    @Test
    @DisplayName("U61: syncToElasticsearch - Domain modelini document'e çevirip indeksler (reindex yolu)")
    void syncToElasticsearch_WhenReviewGiven_ShouldSaveMappedDocument() {
        Review review = ReviewTestFixtures.review("r-1", "prod-1", "cust-1", 5, ReviewStatus.ACTIVE);

        adapter().syncToElasticsearch(review);

        ArgumentCaptor<ReviewDocument> captor = ArgumentCaptor.forClass(ReviewDocument.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("r-1");
        assertThat(captor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(captor.getValue().getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("U62: findById - Document bulunursa domain modeline çevrilir")
    void findById_WhenDocumentExists_ShouldReturnDomainModel() {
        when(repository.findById("r-1")).thenReturn(
                Optional.of(ReviewTestFixtures.reviewDocument("r-1", "prod-1", "cust-1", 4, "ACTIVE")));

        Optional<Review> result = adapter().findById("r-1");

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(ReviewStatus.ACTIVE);
    }

    @Test
    @DisplayName("U63: findById - Document yoksa boş Optional döner")
    void findById_WhenDocumentMissing_ShouldReturnEmpty() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThat(adapter().findById("missing")).isEmpty();
    }

    @Test
    @DisplayName("U64: findActiveByProductId - Sorguya her zaman ACTIVE filtresi eklenir (HIDDEN sızmaz)")
    void findActiveByProductId_ShouldAlwaysQueryWithActiveStatusFilter() {
        when(repository.findByProductIdAndStatus("prod-1", "ACTIVE")).thenReturn(
                List.of(ReviewTestFixtures.reviewDocument("r-1", "prod-1", "cust-1", 5, "ACTIVE")));

        List<Review> result = adapter().findActiveByProductId("prod-1");

        assertThat(result).hasSize(1);
        verify(repository).findByProductIdAndStatus("prod-1", "ACTIVE");
    }

    @Test
    @DisplayName("U65: getAverageRatingForProduct - ACTIVE yorumların puan ortalamasını hesaplar")
    void getAverageRatingForProduct_WhenReviewsExist_ShouldComputeAverage() {
        when(repository.findByProductIdAndStatus("prod-1", "ACTIVE")).thenReturn(List.of(
                ReviewTestFixtures.reviewDocument("r-1", "prod-1", "cust-1", 5, "ACTIVE"),
                ReviewTestFixtures.reviewDocument("r-2", "prod-1", "cust-2", 2, "ACTIVE")));

        assertThat(adapter().getAverageRatingForProduct("prod-1")).isEqualTo(3.5);
    }

    @Test
    @DisplayName("U66: getAverageRatingForProduct - Hiç ACTIVE yorum yoksa 0.0 döner (bölme guard'ı)")
    void getAverageRatingForProduct_WhenNoActiveReviews_ShouldReturnZero() {
        when(repository.findByProductIdAndStatus("prod-x", "ACTIVE")).thenReturn(List.of());

        assertThat(adapter().getAverageRatingForProduct("prod-x")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("U67: findByCustomerId - Müşterinin tüm yorumlarını domain modeline çevirir")
    void findByCustomerId_WhenReviewsExist_ShouldMapAll() {
        when(repository.findByCustomerId("cust-1")).thenReturn(List.of(
                ReviewTestFixtures.reviewDocument("r-1", "prod-1", "cust-1", 5, "ACTIVE"),
                ReviewTestFixtures.reviewDocument("r-2", "prod-2", "cust-1", 1, "HIDDEN")));

        assertThat(adapter().findByCustomerId("cust-1"))
                .extracting(Review::getStatus)
                .containsExactly(ReviewStatus.ACTIVE, ReviewStatus.HIDDEN);
    }

    @Test
    @DisplayName("U68: findAll - Iterable sonucu listeye toplayıp domain modeline çevirir")
    void findAll_WhenDocumentsExist_ShouldCollectIterableIntoList() {
        when(repository.findAll()).thenReturn(List.of(
                ReviewTestFixtures.reviewDocument("r-1", "prod-1", "cust-1", 5, "ACTIVE"),
                ReviewTestFixtures.reviewDocument("r-2", "prod-2", "cust-2", 3, "HIDDEN")));

        assertThat(adapter().findAll()).extracting(Review::getId).containsExactly("r-1", "r-2");
    }

    @Test
    @DisplayName("U69: findAll - İndeks boşsa boş liste döner")
    void findAll_WhenIndexIsEmpty_ShouldReturnEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(adapter().findAll()).isEmpty();
        verify(mapper, org.mockito.Mockito.never()).toDomain(any(ReviewDocument.class));
    }
}
