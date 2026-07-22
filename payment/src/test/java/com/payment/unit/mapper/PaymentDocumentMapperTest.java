package com.payment.unit.mapper;

import com.payment.domain.model.Payment;
import com.payment.domain.model.PaymentStatus;
import com.payment.infrastructure.search.document.PaymentDocument;
import com.payment.infrastructure.search.mapper.PaymentDocumentMapper;
import com.payment.support.PaymentTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Katman: UNIT - domain <-> Elasticsearch document dönüşümü.
 * ES kimliği String, domain kimliği UUID'dir; dönüşümün iki yönde de kayıpsız
 * olması okuma modelinin doğruluğu için kritiktir.
 */
@DisplayName("UNIT - PaymentDocumentMapper (domain <-> ES document)")
class PaymentDocumentMapperTest {

    private final PaymentDocumentMapper mapper = new PaymentDocumentMapper();

    @Test
    @DisplayName("U42: toDocument - UUID kimlik String'e çevrilir, durum adı yazılır")
    void toDocument_WhenDomainIsGiven_ShouldStringifyIdAndStatus() {
        UUID id = UUID.randomUUID();
        Payment domain = PaymentTestFixtures.payment(id, UUID.randomUUID(), UUID.randomUUID(),
                "250.75", PaymentStatus.COMPLETED);

        PaymentDocument document = mapper.toDocument(domain);

        assertThat(document.getId()).isEqualTo(id.toString());
        assertThat(document.getStatus()).isEqualTo("COMPLETED");
        assertThat(document.getAmount()).isEqualByComparingTo(new BigDecimal("250.75"));
    }

    @Test
    @DisplayName("U43: toDomain - String kimlik UUID'ye, durum adı enum'a çevrilir")
    void toDomain_WhenDocumentIsGiven_ShouldParseIdAndStatus() {
        UUID id = UUID.randomUUID();
        PaymentDocument document = PaymentTestFixtures.document(id, UUID.randomUUID(), UUID.randomUUID(),
                "9.90", PaymentStatus.REFUND_REQUESTED);

        Payment domain = mapper.toDomain(document);

        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getStatus()).isEqualTo(PaymentStatus.REFUND_REQUESTED);
        assertThat(domain.getAmount()).isEqualByComparingTo(new BigDecimal("9.90"));
    }

    @Test
    @DisplayName("U44: toDocument - Kimliği olmayan domain için document kimliği null kalır")
    void toDocument_WhenDomainIdIsNull_ShouldLeaveDocumentIdNull() {
        Payment domain = new Payment(null, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1.00"), PaymentStatus.PENDING);

        assertThat(mapper.toDocument(domain).getId()).isNull();
    }

    @Test
    @DisplayName("U45: toDomain - Durum alanı null olan document için domain durumu null olur")
    void toDomain_WhenDocumentStatusIsNull_ShouldReturnNullStatus() {
        PaymentDocument document = PaymentTestFixtures.document(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "1.00", null);

        assertThat(mapper.toDomain(document).getStatus()).isNull();
    }

    @Test
    @DisplayName("U46: toDomain/toDocument - null giriş null çıkış üretir")
    void mapper_WhenInputIsNull_ShouldReturnNull() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toDocument(null)).isNull();
    }

    @Test
    @DisplayName("U47: toDomain - Bilinmeyen durum adı sessizce yutulmaz, hata fırlatılır")
    void toDomain_WhenStatusIsUnknown_ShouldThrowInsteadOfSilentlyIgnoring() {
        PaymentDocument document = new PaymentDocument();
        document.setId(UUID.randomUUID().toString());
        document.setStatus("SOMETHING_ELSE");

        assertThatThrownBy(() -> mapper.toDomain(document))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "durum={0}")
    @EnumSource(PaymentStatus.class)
    @DisplayName("U48: Gidiş-dönüş dönüşümü tüm durumlar için kayıpsızdır")
    void roundTrip_ForEveryStatus_ShouldBeLossless(PaymentStatus status) {
        Payment original = PaymentTestFixtures.payment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "123.45", status);

        Payment roundTripped = mapper.toDomain(mapper.toDocument(original));

        assertThat(roundTripped.getId()).isEqualTo(original.getId());
        assertThat(roundTripped.getOrderId()).isEqualTo(original.getOrderId());
        assertThat(roundTripped.getAmount()).isEqualByComparingTo(original.getAmount());
        assertThat(roundTripped.getStatus()).isEqualTo(status);
    }
}
