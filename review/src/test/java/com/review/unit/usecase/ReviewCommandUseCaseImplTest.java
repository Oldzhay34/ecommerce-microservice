package com.review.unit.usecase;

import com.review.api.dto.CreateReviewRequest;
import com.review.api.dto.ModerateReviewRequest;
import com.review.api.dto.StoreReplyRequest;
import com.review.application.port.out.PurchaseEligibilityPort;
import com.review.application.port.out.ReviewCommandPort;
import com.review.application.port.out.ReviewQueryPort;
import com.review.application.usecase.ReviewCommandUseCaseImpl;
import com.review.domain.exception.DuplicateReviewException;
import com.review.domain.exception.ReviewNotEligibleException;
import com.review.domain.exception.ReviewNotFoundException;
import com.review.domain.model.PurchaseEligibility;
import com.review.domain.model.Review;
import com.review.domain.model.ReviewStatus;
import com.review.unit.support.ReviewTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Saf birim testleri: tüm out-port'lar mock'lanır, hiçbir altyapı (DB, ES,
 * RabbitMQ, Spring context) ayağa kalkmaz. Hedef, use case içindeki HER
 * dallanma/guard'ın (whitebox) kapsanmasıdır.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Review Service - Unit: ReviewCommandUseCaseImpl")
class ReviewCommandUseCaseImplTest {

    @Mock
    private ReviewCommandPort commandPort;

    @Mock
    private ReviewQueryPort queryPort;

    @Mock
    private PurchaseEligibilityPort eligibilityPort;

    @InjectMocks
    private ReviewCommandUseCaseImpl useCase;

    private CreateReviewRequest createRequest;
    private PurchaseEligibility pendingEligibility;

    @BeforeEach
    void setUp() {
        createRequest = ReviewTestFixtures.createReviewRequest("order-1", "prod-1", 5, "Harika ürün");
        pendingEligibility = ReviewTestFixtures.eligibility("elig-1", "order-1", "cust-1", "prod-1", "PENDING_REVIEW");
    }

    @Test
    @DisplayName("U1: createReview - Uygunluk PENDING_REVIEW ise yorumu ACTIVE olarak kaydeder")
    void createReview_WhenEligibilityIsPending_ShouldSaveReviewAsActive() {
        when(eligibilityPort.findPendingEligibility("order-1", "cust-1", "prod-1"))
                .thenReturn(Optional.of(pendingEligibility));
        when(commandPort.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId("review-1");
            return r;
        });

        String reviewId = useCase.createReview("cust-1", createRequest);

        assertThat(reviewId).isEqualTo("review-1");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(commandPort).save(captor.capture());
        Review saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ReviewStatus.ACTIVE);
        assertThat(saved.getCustomerId()).isEqualTo("cust-1");
        assertThat(saved.getProductId()).isEqualTo("prod-1");
        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getComment()).isEqualTo("Harika ürün");
    }

    @Test
    @DisplayName("U2: createReview - Yorum kaydedildikten sonra uygunluk REVIEWED olarak işaretlenir")
    void createReview_WhenSuccessful_ShouldMarkEligibilityAsReviewed() {
        when(eligibilityPort.findPendingEligibility(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(pendingEligibility));
        when(commandPort.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId("review-1");
            return r;
        });

        useCase.createReview("cust-1", createRequest);

        verify(eligibilityPort).markAsReviewed("elig-1");
    }

    @Test
    @DisplayName("U3: createReview - review.created outbox event'ini productId ve rating payload'ı ile yazar")
    void createReview_WhenSuccessful_ShouldWriteReviewCreatedOutboxEvent() {
        when(eligibilityPort.findPendingEligibility(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(pendingEligibility));
        when(commandPort.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId("review-1");
            return r;
        });

        useCase.createReview("cust-1", createRequest);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(commandPort).saveOutboxEvent(eq("review-1"), eq("review.created"), payload.capture());
        assertThat(payload.getValue())
                .contains("\"productId\":\"prod-1\"")
                .contains("\"rating\":5");
    }

    @Test
    @DisplayName("U4: createReview - Uygunluk kaydı yoksa ReviewNotEligibleException fırlatır ve hiçbir şey kaydetmez")
    void createReview_WhenNoEligibilityExists_ShouldThrowReviewNotEligibleException() {
        when(eligibilityPort.findPendingEligibility(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.createReview("cust-1", createRequest))
                .isInstanceOf(ReviewNotEligibleException.class)
                .hasMessageContaining("No pending eligibility");

        verify(commandPort, never()).save(any());
        verify(commandPort, never()).saveOutboxEvent(anyString(), anyString(), anyString());
        verify(eligibilityPort, never()).markAsReviewed(anyString());
    }

    @Test
    @DisplayName("U5: createReview - Uygunluk zaten REVIEWED ise DuplicateReviewException fırlatır")
    void createReview_WhenEligibilityAlreadyReviewed_ShouldThrowDuplicateReviewException() {
        PurchaseEligibility reviewed =
                ReviewTestFixtures.eligibility("elig-1", "order-1", "cust-1", "prod-1", "REVIEWED");
        when(eligibilityPort.findPendingEligibility(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(reviewed));

        assertThatThrownBy(() -> useCase.createReview("cust-1", createRequest))
                .isInstanceOf(DuplicateReviewException.class)
                .hasMessageContaining("already exists");

        verify(commandPort, never()).save(any());
        verify(eligibilityPort, never()).markAsReviewed(anyString());
    }

    @Test
    @DisplayName("U6: createReview - Uygunluk status'ü null ise duplicate guard'ı tetiklenmez ve kayıt yapılır")
    void createReview_WhenEligibilityStatusIsNull_ShouldNotTriggerDuplicateGuard() {
        PurchaseEligibility nullStatus =
                ReviewTestFixtures.eligibility("elig-1", "order-1", "cust-1", "prod-1", null);
        when(eligibilityPort.findPendingEligibility(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(nullStatus));
        when(commandPort.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId("review-1");
            return r;
        });

        assertThat(useCase.createReview("cust-1", createRequest)).isEqualTo("review-1");
    }

    @Test
    @DisplayName("U7: createReview - Yorum metni null olabilir (opsiyonel alan), akış bozulmaz")
    void createReview_WhenCommentIsNull_ShouldStillSaveReview() {
        CreateReviewRequest noComment =
                ReviewTestFixtures.createReviewRequest("order-1", "prod-1", 3, null);
        when(eligibilityPort.findPendingEligibility(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(pendingEligibility));
        when(commandPort.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId("review-2");
            return r;
        });

        useCase.createReview("cust-1", noComment);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(commandPort).save(captor.capture());
        assertThat(captor.getValue().getComment()).isNull();
        assertThat(captor.getValue().getRating()).isEqualTo(3);
    }

    @Test
    @DisplayName("U8: replyToReview - Mağaza cevabını ve cevap zamanını yorum üzerine yazar")
    void replyToReview_WhenReviewExists_ShouldSetReplyTextAndRepliedAt() {
        Review review = ReviewTestFixtures.review("review-1", "prod-1", "cust-1", 4, ReviewStatus.ACTIVE);
        when(queryPort.findById("review-1")).thenReturn(Optional.of(review));
        when(commandPort.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        StoreReplyRequest request = ReviewTestFixtures.storeReplyRequest("Teşekkür ederiz!");

        useCase.replyToReview("review-1", request);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(commandPort).save(captor.capture());
        assertThat(captor.getValue().getStoreReplyText()).isEqualTo("Teşekkür ederiz!");
        assertThat(captor.getValue().getStoreRepliedAt()).isNotNull().isAfter(before);
    }

    @Test
    @DisplayName("U9: replyToReview - Yorum bulunamazsa ReviewNotFoundException fırlatır ve kayıt yapmaz")
    void replyToReview_WhenReviewNotFound_ShouldThrowReviewNotFoundException() {
        when(queryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.replyToReview("missing", ReviewTestFixtures.storeReplyRequest("x")))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("missing");

        verify(commandPort, never()).save(any());
    }

    @Test
    @DisplayName("U10: replyToReview - Var olan cevabın üzerine yazılabilir (idempotent güncelleme)")
    void replyToReview_WhenReplyAlreadyExists_ShouldOverwritePreviousReply() {
        Review review = ReviewTestFixtures.review("review-1", "prod-1", "cust-1", 4, ReviewStatus.ACTIVE);
        review.setStoreReplyText("eski cevap");
        when(queryPort.findById("review-1")).thenReturn(Optional.of(review));
        when(commandPort.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.replyToReview("review-1", ReviewTestFixtures.storeReplyRequest("yeni cevap"));

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(commandPort).save(captor.capture());
        assertThat(captor.getValue().getStoreReplyText()).isEqualTo("yeni cevap");
    }

    @ParameterizedTest(name = "U11-{index}: moderasyon durumu {0}")
    @ValueSource(strings = {"ACTIVE", "HIDDEN"})
    @DisplayName("U11: moderateReview - Geçerli her ReviewStatus değeri için durum geçişi uygulanır")
    void moderateReview_WithValidStatus_ShouldApplyStatusTransition(String status) {
        Review review = ReviewTestFixtures.review("review-1", "prod-1", "cust-1", 4, ReviewStatus.ACTIVE);
        when(queryPort.findById("review-1")).thenReturn(Optional.of(review));
        when(commandPort.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.moderateReview("review-1", ReviewTestFixtures.moderateRequest(status));

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(commandPort).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReviewStatus.valueOf(status));
    }

    @Test
    @DisplayName("U12: moderateReview - HIDDEN yorum tekrar ACTIVE yapılabilir (geri alınabilir moderasyon)")
    void moderateReview_WhenHiddenReviewIsReactivated_ShouldBecomeActive() {
        Review hidden = ReviewTestFixtures.review("review-1", "prod-1", "cust-1", 4, ReviewStatus.HIDDEN);
        when(queryPort.findById("review-1")).thenReturn(Optional.of(hidden));
        when(commandPort.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.moderateReview("review-1", ReviewTestFixtures.moderateRequest("ACTIVE"));

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(commandPort).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReviewStatus.ACTIVE);
    }

    @Test
    @DisplayName("U13: moderateReview - Yorum bulunamazsa ReviewNotFoundException fırlatır")
    void moderateReview_WhenReviewNotFound_ShouldThrowReviewNotFoundException() {
        when(queryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.moderateReview("missing", ReviewTestFixtures.moderateRequest("HIDDEN")))
                .isInstanceOf(ReviewNotFoundException.class);

        verify(commandPort, never()).save(any());
    }

    @Test
    @DisplayName("U14: moderateReview - Tanımsız bir status değeri IllegalArgumentException fırlatır (enum guard)")
    void moderateReview_WithUnknownStatus_ShouldThrowIllegalArgumentException() {
        Review review = ReviewTestFixtures.review("review-1", "prod-1", "cust-1", 4, ReviewStatus.ACTIVE);
        when(queryPort.findById("review-1")).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> useCase.moderateReview("review-1", ReviewTestFixtures.moderateRequest("DELETED")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(commandPort, never()).save(any());
    }

    @Test
    @DisplayName("U15: moderateReview - status null gelirse NPE fırlar; bu yüzden DTO seviyesinde @NotBlank zorunludur")
    void moderateReview_WithNullStatus_ShouldThrowNullPointerException() {
        Review review = ReviewTestFixtures.review("review-1", "prod-1", "cust-1", 4, ReviewStatus.ACTIVE);
        when(queryPort.findById("review-1")).thenReturn(Optional.of(review));

        // Bu, ModerateReviewRequest'e @NotBlank eklenmesinin gerekçesidir:
        // null status use case'e ulaşırsa 400 yerine 500 üretirdi.
        assertThatThrownBy(() -> useCase.moderateReview("review-1", ReviewTestFixtures.moderateRequest(null)))
                .isInstanceOf(NullPointerException.class);

        verify(commandPort, never()).save(any());
    }
}
