package com.review.unit.adapter;

import com.review.domain.model.Review;
import com.review.domain.model.ReviewStatus;
import com.review.infrastructure.persistence.adapter.ReviewPersistenceAdapter;
import com.review.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.review.infrastructure.persistence.entity.ReviewJpaEntity;
import com.review.infrastructure.persistence.mapper.ReviewEntityMapper;
import com.review.infrastructure.persistence.repository.OutboxEventRepository;
import com.review.infrastructure.persistence.repository.ReviewRepository;
import com.review.infrastructure.search.adapter.ReviewSearchAdapter;
import com.review.unit.support.ReviewTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Review Service - Unit: ReviewPersistenceAdapter (CQRS write side)")
class ReviewPersistenceAdapterTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private ReviewEntityMapper mapper = new ReviewEntityMapper();

    @Mock
    private ReviewSearchAdapter searchAdapter;

    private ReviewPersistenceAdapter adapter() {
        return new ReviewPersistenceAdapter(reviewRepository, outboxEventRepository, mapper, searchAdapter);
    }

    @Test
    @DisplayName("U49: save - Yorumu Postgres'e yazar ve ardından Elasticsearch read model'ine senkronlar")
    void save_WhenReviewGiven_ShouldPersistThenSyncToElasticsearch() {
        Review review = ReviewTestFixtures.review(null, "prod-1", "cust-1", 5, ReviewStatus.ACTIVE);
        ReviewJpaEntity persisted =
                ReviewTestFixtures.reviewEntity("r-generated", "prod-1", "cust-1", 5, ReviewStatus.ACTIVE);
        when(reviewRepository.save(any(ReviewJpaEntity.class))).thenReturn(persisted);

        Review result = adapter().save(review);

        assertThat(result.getId()).isEqualTo("r-generated");

        InOrder order = inOrder(reviewRepository, searchAdapter);
        order.verify(reviewRepository).save(any(ReviewJpaEntity.class));
        order.verify(searchAdapter).syncToElasticsearch(any(Review.class));
    }

    @Test
    @DisplayName("U50: save - Elasticsearch'e DB'nin ürettiği id ile senkronlanır (read/write model tutarlılığı)")
    void save_ShouldSyncGeneratedIdToElasticsearch() {
        Review review = ReviewTestFixtures.review(null, "prod-1", "cust-1", 5, ReviewStatus.ACTIVE);
        ReviewJpaEntity persisted =
                ReviewTestFixtures.reviewEntity("r-generated", "prod-1", "cust-1", 5, ReviewStatus.ACTIVE);
        when(reviewRepository.save(any(ReviewJpaEntity.class))).thenReturn(persisted);

        adapter().save(review);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(searchAdapter).syncToElasticsearch(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("r-generated");
        assertThat(captor.getValue().getStatus()).isEqualTo(ReviewStatus.ACTIVE);
    }

    @Test
    @DisplayName("U51: save - Moderasyon güncellemesi de ES'e yansıtılır (HIDDEN reindex)")
    void save_WhenStatusModeratedToHidden_ShouldReindexWithHiddenStatus() {
        Review hidden = ReviewTestFixtures.review("r-1", "prod-1", "cust-1", 5, ReviewStatus.HIDDEN);
        when(reviewRepository.save(any(ReviewJpaEntity.class)))
                .thenReturn(ReviewTestFixtures.reviewEntity("r-1", "prod-1", "cust-1", 5, ReviewStatus.HIDDEN));

        adapter().save(hidden);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(searchAdapter).syncToElasticsearch(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReviewStatus.HIDDEN);
    }

    @Test
    @DisplayName("U52: saveOutboxEvent - Outbox satırını Review aggregate tipiyle ve verilen payload'la yazar")
    void saveOutboxEvent_ShouldPersistOutboxRowWithReviewAggregateType() {
        adapter().saveOutboxEvent("r-1", "review.created", "{\"productId\":\"prod-1\",\"rating\":5}");

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEventJpaEntity event = captor.getValue();
        assertThat(event.getAggregateType()).isEqualTo("Review");
        assertThat(event.getAggregateId()).isEqualTo("r-1");
        assertThat(event.getType()).isEqualTo("review.created");
        assertThat(event.getPayload()).isEqualTo("{\"productId\":\"prod-1\",\"rating\":5}");
    }
}
