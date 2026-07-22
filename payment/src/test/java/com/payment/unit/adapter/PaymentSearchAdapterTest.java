package com.payment.unit.adapter;

import com.payment.domain.model.Payment;
import com.payment.domain.model.PaymentStatus;
import com.payment.infrastructure.search.adapter.PaymentSearchAdapter;
import com.payment.infrastructure.search.mapper.PaymentDocumentMapper;
import com.payment.infrastructure.search.repository.PaymentSearchRepository;
import com.payment.support.PaymentTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT - okuma modeli adapter'ı (PaymentQueryPort implementasyonu).
 * ES kimliği String olduğu için UUID -> String çevriminin doğru yapılması
 * kritiktir; yanlış anahtarla arama "ödeme bulunamadı" yanılgısı üretir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - PaymentSearchAdapter (Elasticsearch okuma modeli)")
class PaymentSearchAdapterTest {

    @Mock
    private PaymentSearchRepository paymentSearchRepository;

    private PaymentSearchAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PaymentSearchAdapter(paymentSearchRepository, new PaymentDocumentMapper());
    }

    @Test
    @DisplayName("U56: findById - UUID, ES'nin beklediği String anahtara çevrilerek sorgulanır")
    void findById_ShouldQueryElasticsearchWithStringifiedUuid() {
        UUID id = UUID.randomUUID();
        when(paymentSearchRepository.findById(id.toString()))
                .thenReturn(Optional.of(PaymentTestFixtures.document(id, UUID.randomUUID(), UUID.randomUUID(),
                        "55.55", PaymentStatus.COMPLETED)));

        Optional<Payment> result = adapter.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
        assertThat(result.get().getAmount()).isEqualByComparingTo(new BigDecimal("55.55"));
        verify(paymentSearchRepository).findById(id.toString());
    }

    @Test
    @DisplayName("U57: findById - Kayıt yoksa Optional.empty döner, istisna fırlatılmaz")
    void findById_WhenDocumentIsMissing_ShouldReturnEmptyOptional() {
        UUID id = UUID.randomUUID();
        when(paymentSearchRepository.findById(id.toString())).thenReturn(Optional.empty());

        assertThat(adapter.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("U58: findByCustomerId - Müşterinin tüm ödemeleri domain nesnesine çevrilerek döner")
    void findByCustomerId_ShouldMapAllDocumentsToDomain() {
        UUID customerId = UUID.randomUUID();
        when(paymentSearchRepository.findByCustomerId(customerId)).thenReturn(List.of(
                PaymentTestFixtures.document(UUID.randomUUID(), UUID.randomUUID(), customerId, "10.00",
                        PaymentStatus.COMPLETED),
                PaymentTestFixtures.document(UUID.randomUUID(), UUID.randomUUID(), customerId, "20.00",
                        PaymentStatus.REFUNDED)));

        List<Payment> result = adapter.findByCustomerId(customerId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Payment::getStatus)
                .containsExactly(PaymentStatus.COMPLETED, PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("U59: findByOrderId - Siparişe ait ödemeler döner")
    void findByOrderId_ShouldReturnPaymentsOfThatOrder() {
        UUID orderId = UUID.randomUUID();
        when(paymentSearchRepository.findByOrderId(orderId)).thenReturn(List.of(
                PaymentTestFixtures.document(UUID.randomUUID(), orderId, UUID.randomUUID(), "33.30",
                        PaymentStatus.COMPLETED)));

        assertThat(adapter.findByOrderId(orderId)).hasSize(1);
    }

    @Test
    @DisplayName("U60: findAll - Iterable sonuç listeye çevrilir, boşsa boş liste döner")
    void findAll_WhenIndexIsEmpty_ShouldReturnEmptyList() {
        when(paymentSearchRepository.findAll()).thenReturn(List.of());

        assertThat(adapter.findAll()).isEmpty();
    }

    @Test
    @DisplayName("U61: findByCustomerId - Kayıt yoksa boş liste döner (null değil)")
    void findByCustomerId_WhenNoDocuments_ShouldReturnEmptyList() {
        UUID customerId = UUID.randomUUID();
        when(paymentSearchRepository.findByCustomerId(customerId)).thenReturn(List.of());

        assertThat(adapter.findByCustomerId(customerId)).isNotNull().isEmpty();
    }
}
