package com.payment.unit.mapper;

import com.payment.domain.model.Payment;
import com.payment.domain.model.PaymentStatus;
import com.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import com.payment.infrastructure.persistence.mapper.PaymentEntityMapper;
import com.payment.support.PaymentTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Katman: UNIT - domain <-> JPA entity dönüşümü. Tutarın ölçeği dönüşümde
 * bozulmamalıdır; para alanları {@code isEqualByComparingTo} ile karşılaştırılır.
 */
@DisplayName("UNIT - PaymentEntityMapper (domain <-> JPA entity)")
class PaymentEntityMapperTest {

    private final PaymentEntityMapper mapper = new PaymentEntityMapper();

    @Test
    @DisplayName("U37: toEntity - Tüm alanlar entity'ye aktarılır, tutar ve ölçek korunur")
    void toEntity_WhenDomainIsGiven_ShouldCopyAllFieldsPreservingScale() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Payment domain = PaymentTestFixtures.payment(id, orderId, customerId, "1234.50", PaymentStatus.COMPLETED);

        PaymentJpaEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getOrderId()).isEqualTo(orderId);
        assertThat(entity.getCustomerId()).isEqualTo(customerId);
        assertThat(entity.getAmount()).isEqualByComparingTo(new BigDecimal("1234.50"));
        assertThat(entity.getAmount().scale()).isEqualTo(2);
        assertThat(entity.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("U38: toDomain - Entity alanları domain'e aktarılır")
    void toDomain_WhenEntityIsGiven_ShouldCopyAllFields() {
        UUID id = UUID.randomUUID();
        PaymentJpaEntity entity = PaymentTestFixtures.entity(id, UUID.randomUUID(), UUID.randomUUID(),
                "0.01", PaymentStatus.REFUNDED);

        Payment domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(id);
        assertThat(domain.getAmount()).isEqualByComparingTo(new BigDecimal("0.01"));
        assertThat(domain.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("U39: toEntity - Yeni ödemede ID null bırakılır (ID'yi veritabanı atar)")
    void toEntity_WhenDomainIdIsNull_ShouldLeaveEntityIdNull() {
        Payment domain = new Payment(null, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("5.00"), PaymentStatus.PENDING);

        assertThat(mapper.toEntity(domain).getId())
                .as("@GeneratedValue(UUID) kullanıldığı için ID uygulama tarafından atanmamalı")
                .isNull();
    }

    @Test
    @DisplayName("U40: toDomain/toEntity - null giriş null çıkış üretir, NPE oluşmaz")
    void mapper_WhenInputIsNull_ShouldReturnNull() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }

    @ParameterizedTest(name = "durum={0}")
    @EnumSource(PaymentStatus.class)
    @DisplayName("U41: Gidiş-dönüş dönüşümü tüm PaymentStatus değerleri için kayıpsızdır")
    void roundTrip_ForEveryStatus_ShouldBeLossless(PaymentStatus status) {
        Payment original = PaymentTestFixtures.payment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "19.99", status);

        Payment roundTripped = mapper.toDomain(mapper.toEntity(original));

        assertThat(roundTripped.getId()).isEqualTo(original.getId());
        assertThat(roundTripped.getOrderId()).isEqualTo(original.getOrderId());
        assertThat(roundTripped.getCustomerId()).isEqualTo(original.getCustomerId());
        assertThat(roundTripped.getAmount()).isEqualByComparingTo(original.getAmount());
        assertThat(roundTripped.getStatus()).isEqualTo(status);
    }
}
