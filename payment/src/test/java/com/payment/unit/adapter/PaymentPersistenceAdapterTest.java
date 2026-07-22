package com.payment.unit.adapter;

import com.payment.domain.model.Payment;
import com.payment.domain.model.PaymentStatus;
import com.payment.infrastructure.persistence.adapter.PaymentPersistenceAdapter;
import com.payment.infrastructure.persistence.entity.OutboxJpaEntity;
import com.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import com.payment.infrastructure.persistence.mapper.PaymentEntityMapper;
import com.payment.infrastructure.persistence.repository.OutboxRepository;
import com.payment.infrastructure.persistence.repository.PaymentRepository;
import com.payment.infrastructure.search.document.PaymentDocument;
import com.payment.infrastructure.search.mapper.PaymentDocumentMapper;
import com.payment.infrastructure.search.repository.PaymentSearchRepository;
import com.payment.support.PaymentTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT - yazma tarafı adapter'ı. Repository'ler mock'lanır; test edilen
 * şey CQRS senkronizasyonudur: her yazma HEM PostgreSQL'e HEM Elasticsearch'e
 * gitmelidir, aksi halde okuma modeli ödemenin gerçek durumunu göstermez.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - PaymentPersistenceAdapter (yazma modeli + okuma modeli senkronu)")
class PaymentPersistenceAdapterTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private PaymentSearchRepository paymentSearchRepository;

    private PaymentPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PaymentPersistenceAdapter(paymentRepository, outboxRepository,
                new PaymentEntityMapper(), paymentSearchRepository, new PaymentDocumentMapper());
    }

    @Test
    @DisplayName("U49: save - Ödeme hem PostgreSQL'e hem Elasticsearch'e yazılır")
    void save_WhenPaymentIsGiven_ShouldPersistToBothWriteAndReadModels() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(paymentRepository.save(any(PaymentJpaEntity.class))).thenAnswer(invocation -> {
            PaymentJpaEntity entity = invocation.getArgument(0);
            entity.setId(id);
            return entity;
        });

        Payment saved = adapter.save(new Payment(null, orderId, customerId,
                new BigDecimal("321.45"), PaymentStatus.PENDING));

        assertThat(saved.getId()).isEqualTo(id);

        ArgumentCaptor<PaymentDocument> documentCaptor = ArgumentCaptor.forClass(PaymentDocument.class);
        verify(paymentSearchRepository).save(documentCaptor.capture());
        assertThat(documentCaptor.getValue().getId()).isEqualTo(id.toString());
        assertThat(documentCaptor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("321.45"));
        assertThat(documentCaptor.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("U50: save - Okuma modeline yazılan kimlik, veritabanının ürettiği kimliktir")
    void save_ShouldIndexUsingDatabaseGeneratedIdNotTheIncomingOne() {
        UUID generated = UUID.randomUUID();
        when(paymentRepository.save(any(PaymentJpaEntity.class))).thenAnswer(invocation -> {
            PaymentJpaEntity entity = invocation.getArgument(0);
            entity.setId(generated);
            return entity;
        });

        adapter.save(new Payment(null, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1.00"), PaymentStatus.PENDING));

        InOrder order = inOrder(paymentRepository, paymentSearchRepository);
        order.verify(paymentRepository).save(any(PaymentJpaEntity.class));
        order.verify(paymentSearchRepository).save(any(PaymentDocument.class));
    }

    @Test
    @DisplayName("U51: save - PostgreSQL yazması patlarsa Elasticsearch'e HİÇ yazılmaz (okuma modeli hayali ödeme göstermez)")
    void save_WhenWriteModelFails_ShouldNotIndexIntoReadModel() {
        when(paymentRepository.save(any(PaymentJpaEntity.class)))
                .thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> adapter.save(new Payment(null, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1.00"), PaymentStatus.PENDING)))
                .isInstanceOf(IllegalStateException.class);

        verify(paymentSearchRepository, never()).save(any(PaymentDocument.class));
    }

    @Test
    @DisplayName("U52: saveOutboxEvent - Olay outbox tablosuna tüm alanlarıyla yazılır")
    void saveOutboxEvent_ShouldPersistOutboxRowWithAllFields() {
        UUID paymentId = UUID.randomUUID();

        adapter.saveOutboxEvent("Payment", paymentId.toString(), "PaymentCompletedEvent", "{\"a\":1}");

        ArgumentCaptor<OutboxJpaEntity> captor = ArgumentCaptor.forClass(OutboxJpaEntity.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getAggregateType()).isEqualTo("Payment");
        assertThat(captor.getValue().getAggregateId()).isEqualTo(paymentId.toString());
        assertThat(captor.getValue().getType()).isEqualTo("PaymentCompletedEvent");
        assertThat(captor.getValue().getPayload()).isEqualTo("{\"a\":1}");
    }

    /**
     * REGRESYON: Idempotency kontrolü bilerek YAZMA MODELİ üzerinden yapılır.
     * Elasticsearch near-real-time olduğu için (varsayılan 1 sn refresh) hızlı
     * tekrar teslimlerde ödemeyi henüz göremez ve çift tahsilata izin verirdi.
     */
    @Test
    @DisplayName("U53: existsPaymentForOrder - Sorgu Elasticsearch'e değil PostgreSQL'e gider")
    void existsPaymentForOrder_ShouldQueryWriteModelNotSearchIndex() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(true);

        assertThat(adapter.existsPaymentForOrder(orderId)).isTrue();

        verify(paymentRepository).existsByOrderId(orderId);
        verify(paymentSearchRepository, never()).findByOrderId(any(UUID.class));
    }

    @Test
    @DisplayName("U54: existsPaymentForOrder - Kayıt yoksa false döner")
    void existsPaymentForOrder_WhenNoPaymentExists_ShouldReturnFalse() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);

        assertThat(adapter.existsPaymentForOrder(orderId)).isFalse();
    }

    @Test
    @DisplayName("U55: save - Kuruş hassasiyeti iki modele de aynı şekilde yazılır")
    void save_ShouldPreserveMinorUnitPrecisionInBothModels() {
        when(paymentRepository.save(any(PaymentJpaEntity.class))).thenAnswer(invocation -> {
            PaymentJpaEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        adapter.save(PaymentTestFixtures.payment(null, UUID.randomUUID(), UUID.randomUUID(),
                "0.01", PaymentStatus.COMPLETED));

        ArgumentCaptor<PaymentJpaEntity> entityCaptor = ArgumentCaptor.forClass(PaymentJpaEntity.class);
        ArgumentCaptor<PaymentDocument> documentCaptor = ArgumentCaptor.forClass(PaymentDocument.class);
        verify(paymentRepository).save(entityCaptor.capture());
        verify(paymentSearchRepository).save(documentCaptor.capture());

        assertThat(entityCaptor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("0.01"));
        assertThat(documentCaptor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("0.01"));
    }
}
