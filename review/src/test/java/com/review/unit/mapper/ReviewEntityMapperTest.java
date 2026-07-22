package com.review.unit.mapper;

import com.review.domain.model.Review;
import com.review.domain.model.ReviewStatus;
import com.review.infrastructure.persistence.entity.ReviewJpaEntity;
import com.review.infrastructure.persistence.mapper.ReviewEntityMapper;
import com.review.unit.support.ReviewTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Review Service - Unit: ReviewEntityMapper (domain <-> JPA entity)")
class ReviewEntityMapperTest {

    private final ReviewEntityMapper mapper = new ReviewEntityMapper();

    @Test
    @DisplayName("U22: toDomain - Entity'nin tüm alanlarını domain modeline taşır")
    void toDomain_WhenEntityGiven_ShouldMapEveryField() {
        ReviewJpaEntity entity =
                ReviewTestFixtures.reviewEntity("r-1", "prod-1", "cust-1", 4, ReviewStatus.HIDDEN);
        entity.setStoreReplyText("cevap");
        entity.setStoreRepliedAt(LocalDateTime.of(2026, 2, 1, 8, 0));

        Review domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo("r-1");
        assertThat(domain.getProductId()).isEqualTo("prod-1");
        assertThat(domain.getCustomerId()).isEqualTo("cust-1");
        assertThat(domain.getRating()).isEqualTo(4);
        assertThat(domain.getComment()).isEqualTo("yorum-r-1");
        assertThat(domain.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        assertThat(domain.getStoreReplyText()).isEqualTo("cevap");
        assertThat(domain.getStoreRepliedAt()).isEqualTo(LocalDateTime.of(2026, 2, 1, 8, 0));
        assertThat(domain.getCreatedAt()).isEqualTo(ReviewTestFixtures.FIXED_CREATED_AT);
        assertThat(domain.getUpdatedAt()).isEqualTo(ReviewTestFixtures.FIXED_CREATED_AT);
    }

    @Test
    @DisplayName("U23: toEntity - Domain modelinin tüm alanlarını entity'ye taşır")
    void toEntity_WhenDomainGiven_ShouldMapEveryField() {
        Review domain = ReviewTestFixtures.review("r-2", "prod-2", "cust-2", 2, ReviewStatus.ACTIVE);
        domain.setStoreReplyText("cevap-2");

        ReviewJpaEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo("r-2");
        assertThat(entity.getProductId()).isEqualTo("prod-2");
        assertThat(entity.getCustomerId()).isEqualTo("cust-2");
        assertThat(entity.getRating()).isEqualTo(2);
        assertThat(entity.getStatus()).isEqualTo(ReviewStatus.ACTIVE);
        assertThat(entity.getStoreReplyText()).isEqualTo("cevap-2");
        assertThat(entity.getCreatedAt()).isEqualTo(ReviewTestFixtures.FIXED_CREATED_AT);
    }

    @Test
    @DisplayName("U24: toEntity/toDomain - Gidiş-dönüş dönüşümü bilgi kaybetmez")
    void roundTrip_WhenDomainMappedBackAndForth_ShouldPreserveAllFields() {
        Review original = ReviewTestFixtures.review("r-3", "prod-3", "cust-3", 5, ReviewStatus.ACTIVE);

        Review roundTripped = mapper.toDomain(mapper.toEntity(original));

        assertThat(roundTripped).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    @DisplayName("U25: toDomain - null entity için null döner (guard)")
    void toDomain_WhenEntityIsNull_ShouldReturnNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    @DisplayName("U26: toEntity - null domain için null döner (guard)")
    void toEntity_WhenDomainIsNull_ShouldReturnNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("U27: toEntity - Yeni yorumda id null bırakılır, ID üretimi JPA'ya devredilir")
    void toEntity_WhenDomainIdIsNull_ShouldLeaveEntityIdNull() {
        Review domain = new Review();
        domain.setProductId("prod-1");
        domain.setCustomerId("cust-1");
        domain.setRating(5);
        domain.setStatus(ReviewStatus.ACTIVE);

        assertThat(mapper.toEntity(domain).getId()).isNull();
    }
}
