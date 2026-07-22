package com.payment.unit.gateway;

import com.payment.domain.exception.PaymentGatewayNotConfiguredException;
import com.payment.infrastructure.gateway.iyzico.IyzicoPaymentGatewayAdapter;
import com.payment.support.PaymentTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Katman: UNIT - Iyzico adapter'ı.
 * <p>
 * DİKKAT: Bu adapter production'da HENÜZ BİR ENTEGRASYON İÇERMEZ; her çağrıda
 * {@link PaymentGatewayNotConfiguredException} fırlatır. Testler var olmayan bir
 * davranışı varsaymaz; kanıtladıkları şey "yanlış yapılandırılmış gateway
 * sessizce başarılı sayılmaz ve harici sisteme İSTEK GİTMEZ"dir.
 * Gerçek soket davranışı (5xx / timeout / bozuk JSON) integration katmanında
 * MockWebServer ile ayrıca test edilir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - IyzicoPaymentGatewayAdapter (yapılandırılmamış gateway)")
class IyzicoPaymentGatewayAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    @DisplayName("U34: charge - Entegrasyon tamamlanmadığı için PaymentGatewayNotConfiguredException fırlatılır")
    void charge_WhenIntegrationIsNotConfigured_ShouldThrowPaymentGatewayNotConfigured() {
        IyzicoPaymentGatewayAdapter adapter = new IyzicoPaymentGatewayAdapter(restTemplate, "key", "secret");

        assertThatThrownBy(() -> adapter.charge(
                PaymentTestFixtures.chargeCommand(UUID.randomUUID(), UUID.randomUUID(), "100.00")))
                .isInstanceOf(PaymentGatewayNotConfiguredException.class)
                .hasMessageContaining("not fully configured");
    }

    @Test
    @DisplayName("U35: charge - Anahtarlar boş olsa bile davranış aynıdır, hata yutulmaz")
    void charge_WhenApiKeysAreBlank_ShouldStillThrowInsteadOfSilentlySucceeding() {
        IyzicoPaymentGatewayAdapter adapter = new IyzicoPaymentGatewayAdapter(restTemplate, "", "");

        assertThatThrownBy(() -> adapter.charge(
                PaymentTestFixtures.chargeCommand(UUID.randomUUID(), UUID.randomUUID(), "1.00")))
                .isInstanceOf(PaymentGatewayNotConfiguredException.class);
    }

    @Test
    @DisplayName("U36: charge - Harici sisteme HİÇBİR HTTP isteği gönderilmez (yanlış tahsilat riski yok)")
    void charge_ShouldNotIssueAnyHttpCallToProvider() {
        IyzicoPaymentGatewayAdapter adapter = new IyzicoPaymentGatewayAdapter(restTemplate, "key", "secret");

        assertThatThrownBy(() -> adapter.charge(
                PaymentTestFixtures.chargeCommand(UUID.randomUUID(), UUID.randomUUID(), "100.00")))
                .isInstanceOf(PaymentGatewayNotConfiguredException.class);

        verifyNoInteractions(restTemplate);
    }
}
