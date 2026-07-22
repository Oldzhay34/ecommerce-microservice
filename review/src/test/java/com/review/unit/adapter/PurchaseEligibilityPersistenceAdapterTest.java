package com.review.unit.adapter;

import com.review.domain.model.PurchaseEligibility;
import com.review.infrastructure.persistence.adapter.PurchaseEligibilityPersistenceAdapter;
import com.review.infrastructure.persistence.entity.PurchaseEligibilityJpaEntity;
import com.review.infrastructure.persistence.repository.PurchaseEligibilityRepository;
import com.review.unit.support.ReviewTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Review Service - Unit: PurchaseEligibilityPersistenceAdapter (satın alma uygunluğu)")
class PurchaseEligibilityPersistenceAdapterTest {

    @Mock
    private PurchaseEligibilityRepository repository;

    @InjectMocks
    private PurchaseEligibilityPersistenceAdapter adapter;

    @Test
    @DisplayName("U53: findPendingEligibility - PENDING_REVIEW kaydı domain modeline çevrilerek döner")
    void findPendingEligibility_WhenStatusIsPending_ShouldReturnDomainModel() {
        PurchaseEligibilityJpaEntity entity =
                ReviewTestFixtures.eligibilityEntity("order-1", "cust-1", "prod-1", "PENDING_REVIEW");
        entity.setId("elig-1");
        when(repository.findByOrderIdAndCustomerIdAndProductId("order-1", "cust-1", "prod-1"))
                .thenReturn(Optional.of(entity));

        Optional<PurchaseEligibility> result =
                adapter.findPendingEligibility("order-1", "cust-1", "prod-1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("elig-1");
        assertThat(result.get().getStatus()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    @DisplayName("U54: findPendingEligibility - Kayıt REVIEWED ise filtrelenir ve boş döner (duplicate guard)")
    void findPendingEligibility_WhenStatusIsReviewed_ShouldReturnEmpty() {
        PurchaseEligibilityJpaEntity entity =
                ReviewTestFixtures.eligibilityEntity("order-1", "cust-1", "prod-1", "REVIEWED");
        when(repository.findByOrderIdAndCustomerIdAndProductId("order-1", "cust-1", "prod-1"))
                .thenReturn(Optional.of(entity));

        assertThat(adapter.findPendingEligibility("order-1", "cust-1", "prod-1")).isEmpty();
    }

    @Test
    @DisplayName("U55: findPendingEligibility - Hiç kayıt yoksa boş döner (satın almadan yorum engellenir)")
    void findPendingEligibility_WhenNoRecord_ShouldReturnEmpty() {
        when(repository.findByOrderIdAndCustomerIdAndProductId("order-x", "cust-x", "prod-x"))
                .thenReturn(Optional.empty());

        assertThat(adapter.findPendingEligibility("order-x", "cust-x", "prod-x")).isEmpty();
    }

    @Test
    @DisplayName("U56: markAsReviewed - Kayıt varsa status REVIEWED yapılıp kaydedilir")
    void markAsReviewed_WhenRecordExists_ShouldUpdateStatusToReviewed() {
        PurchaseEligibilityJpaEntity entity =
                ReviewTestFixtures.eligibilityEntity("order-1", "cust-1", "prod-1", "PENDING_REVIEW");
        when(repository.findById("elig-1")).thenReturn(Optional.of(entity));

        adapter.markAsReviewed("elig-1");

        ArgumentCaptor<PurchaseEligibilityJpaEntity> captor =
                ArgumentCaptor.forClass(PurchaseEligibilityJpaEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("REVIEWED");
    }

    @Test
    @DisplayName("U57: markAsReviewed - Kayıt yoksa sessizce geçer, save çağrılmaz")
    void markAsReviewed_WhenRecordMissing_ShouldDoNothing() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        adapter.markAsReviewed("missing");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("U58: createIdempotent - Kayıt yoksa PENDING_REVIEW olarak yaratılır")
    void createIdempotent_WhenNoRecordExists_ShouldCreatePendingReview() {
        when(repository.findByOrderIdAndCustomerIdAndProductId("order-1", "cust-1", "prod-1"))
                .thenReturn(Optional.empty());

        adapter.createIdempotent("order-1", "cust-1", "prod-1");

        ArgumentCaptor<PurchaseEligibilityJpaEntity> captor =
                ArgumentCaptor.forClass(PurchaseEligibilityJpaEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo("order-1");
        assertThat(captor.getValue().getCustomerId()).isEqualTo("cust-1");
        assertThat(captor.getValue().getProductId()).isEqualTo("prod-1");
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    @DisplayName("U59: createIdempotent - Kayıt zaten varsa tekrar yaratılmaz (event tekrarına dayanıklı)")
    void createIdempotent_WhenRecordAlreadyExists_ShouldNotCreateDuplicate() {
        when(repository.findByOrderIdAndCustomerIdAndProductId("order-1", "cust-1", "prod-1"))
                .thenReturn(Optional.of(
                        ReviewTestFixtures.eligibilityEntity("order-1", "cust-1", "prod-1", "PENDING_REVIEW")));

        adapter.createIdempotent("order-1", "cust-1", "prod-1");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("U60: createIdempotent - Kayıt REVIEWED durumundaysa da tekrar yaratılmaz (yeniden yorum engellenir)")
    void createIdempotent_WhenRecordAlreadyReviewed_ShouldNotResetStatus() {
        when(repository.findByOrderIdAndCustomerIdAndProductId("order-1", "cust-1", "prod-1"))
                .thenReturn(Optional.of(
                        ReviewTestFixtures.eligibilityEntity("order-1", "cust-1", "prod-1", "REVIEWED")));

        adapter.createIdempotent("order-1", "cust-1", "prod-1");

        verify(repository, never()).save(any());
    }
}
