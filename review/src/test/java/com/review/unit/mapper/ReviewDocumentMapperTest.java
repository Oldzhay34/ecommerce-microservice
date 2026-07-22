package com.review.unit.mapper;

import com.review.domain.model.Review;
import com.review.domain.model.ReviewStatus;
import com.review.infrastructure.search.document.ReviewDocument;
import com.review.infrastructure.search.mapper.ReviewDocumentMapper;
import com.review.unit.support.ReviewTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Review Service - Unit: ReviewDocumentMapper (domain <-> Elasticsearch document)")
class ReviewDocumentMapperTest {

    private final ReviewDocumentMapper mapper = new ReviewDocumentMapper();

    @ParameterizedTest(name = "U28-{index}: {0} durumu document'e string olarak yazılır")
    @ValueSource(strings = {"ACTIVE", "HIDDEN"})
    @DisplayName("U28: toDocument - Her ReviewStatus değeri document'e adıyla yazılır")
    void toDocument_WithEachStatus_ShouldWriteStatusName(String status) {
        Review review = ReviewTestFixtures.review("r-1", "prod-1", "cust-1", 4, ReviewStatus.valueOf(status));

        assertThat(mapper.toDocument(review).getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("U29: toDocument - Tüm alanları Elasticsearch document'ine taşır")
    void toDocument_WhenReviewGiven_ShouldMapEveryField() {
        Review review = ReviewTestFixtures.review("r-1", "prod-1", "cust-1", 4, ReviewStatus.ACTIVE);
        review.setStoreReplyText("mağaza cevabı");

        ReviewDocument doc = mapper.toDocument(review);

        assertThat(doc.getId()).isEqualTo("r-1");
        assertThat(doc.getProductId()).isEqualTo("prod-1");
        assertThat(doc.getCustomerId()).isEqualTo("cust-1");
        assertThat(doc.getRating()).isEqualTo(4);
        assertThat(doc.getComment()).isEqualTo("yorum-r-1");
        assertThat(doc.getStoreReplyText()).isEqualTo("mağaza cevabı");
        assertThat(doc.getCreatedAt()).isEqualTo(ReviewTestFixtures.FIXED_CREATED_AT);
    }

    @Test
    @DisplayName("U30: toDocument - status null ise document status'ü de null olur (NPE guard)")
    void toDocument_WhenStatusIsNull_ShouldWriteNullStatusWithoutThrowing() {
        Review review = new Review();
        review.setId("r-1");
        review.setStatus(null);

        assertThat(mapper.toDocument(review).getStatus()).isNull();
    }

    @Test
    @DisplayName("U31: toDomain - Document status'ü enum'a çevrilir")
    void toDomain_WhenDocumentHasStatus_ShouldParseEnum() {
        ReviewDocument doc = ReviewTestFixtures.reviewDocument("r-1", "prod-1", "cust-1", 3, "HIDDEN");

        Review review = mapper.toDomain(doc);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        assertThat(review.getId()).isEqualTo("r-1");
        assertThat(review.getRating()).isEqualTo(3);
        assertThat(review.getCreatedAt()).isEqualTo(ReviewTestFixtures.FIXED_CREATED_AT);
    }

    @Test
    @DisplayName("U32: toDomain - Document status'ü null ise domain status'ü null bırakılır")
    void toDomain_WhenDocumentStatusIsNull_ShouldLeaveDomainStatusNull() {
        ReviewDocument doc = ReviewTestFixtures.reviewDocument("r-1", "prod-1", "cust-1", 3, null);

        assertThat(mapper.toDomain(doc).getStatus()).isNull();
    }

    @Test
    @DisplayName("U33: toDocument/toDomain - null girdiler için null döner (guard)")
    void mapper_WhenInputIsNull_ShouldReturnNull() {
        assertThat(mapper.toDocument(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    @DisplayName("U34: toDomain - ES document'i updatedAt taşımaz, dönüşte null kalır (CQRS read model sınırı)")
    void toDomain_WhenMappedFromDocument_ShouldLeaveUpdatedAtNull() {
        ReviewDocument doc = ReviewTestFixtures.reviewDocument("r-1", "prod-1", "cust-1", 3, "ACTIVE");

        assertThat(mapper.toDomain(doc).getUpdatedAt()).isNull();
    }
}
