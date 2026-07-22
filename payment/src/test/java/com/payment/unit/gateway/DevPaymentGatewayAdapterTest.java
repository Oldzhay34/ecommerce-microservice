package com.payment.unit.gateway;

import com.payment.application.port.out.PaymentGatewayResult;
import com.payment.domain.model.PaymentStatus;
import com.payment.infrastructure.gateway.dev.DevPaymentGatewayAdapter;
import com.payment.support.PaymentTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Katman: UNIT - dev (simülasyon) gateway adapter'ı.
 * Sınır değeri 10000'dir ve karşılaştırma {@code compareTo} ile yapılmalıdır:
 * "10000" ile "10000.00" ölçekleri farklı olduğu için {@code equals} ile aynı
 * sayılmaz, ancak İKİSİ DE limit içinde kabul edilmelidir.
 */
@DisplayName("UNIT - DevPaymentGatewayAdapter (simülasyon gateway'i, tutar limiti)")
class DevPaymentGatewayAdapterTest {

    private final DevPaymentGatewayAdapter adapter = new DevPaymentGatewayAdapter();

    @ParameterizedTest(name = "tutar={0} -> {1}")
    @CsvSource({
            "0.01,      COMPLETED",
            "1,         COMPLETED",
            "9999.99,   COMPLETED",
            "10000,     COMPLETED",
            "10000.00,  COMPLETED",
            "10000.000, COMPLETED",
            "10000.01,  FAILED",
            "10001,     FAILED",
            "99999.99,  FAILED"
    })
    @DisplayName("U30: charge - Limit karşılaştırması ölçekten bağımsız yapılır (compareTo semantiği)")
    void charge_WhenAmountComparedToLimit_ShouldIgnoreScaleDifferences(String amount, PaymentStatus expected) {
        PaymentGatewayResult result = adapter.charge(
                PaymentTestFixtures.chargeCommand(UUID.randomUUID(), UUID.randomUUID(), amount));

        assertThat(result.status()).isEqualTo(expected);
    }

    @Test
    @DisplayName("U31: charge - Başarılı tahsilat DEV önekli bir işlem kimliği üretir")
    void charge_WhenSuccessful_ShouldReturnDevPrefixedTransactionId() {
        PaymentGatewayResult result = adapter.charge(
                PaymentTestFixtures.chargeCommand(UUID.randomUUID(), UUID.randomUUID(), "100.00"));

        assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.providerTransactionId()).startsWith("DEV-");
        assertThat(result.rawResponseMessage()).isEqualTo("Simulated success");
    }

    @Test
    @DisplayName("U32: charge - Her çağrı benzersiz bir işlem kimliği üretir (çift tahsilat izlenebilsin)")
    void charge_WhenCalledTwice_ShouldProduceDistinctTransactionIds() {
        var command = PaymentTestFixtures.chargeCommand(UUID.randomUUID(), UUID.randomUUID(), "100.00");

        assertThat(adapter.charge(command).providerTransactionId())
                .isNotEqualTo(adapter.charge(command).providerTransactionId());
    }

    @Test
    @DisplayName("U33: charge - Limit aşıldığında FAILED döner ve sebebi mesajda belirtilir")
    void charge_WhenAmountExceedsLimit_ShouldReturnFailedWithReason() {
        PaymentGatewayResult result = adapter.charge(
                PaymentTestFixtures.chargeCommand(UUID.randomUUID(), UUID.randomUUID(), "10000.01"));

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.rawResponseMessage()).isEqualTo("Amount exceeds dev limit");
    }
}
