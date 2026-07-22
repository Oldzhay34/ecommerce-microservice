package com.payment.integration;

import com.payment.application.port.out.PaymentGatewayPort;
import com.payment.domain.exception.PaymentGatewayNotConfiguredException;
import com.payment.domain.model.PaymentStatus;
import com.payment.infrastructure.config.RestTemplateConfig;
import com.payment.infrastructure.gateway.dev.DevPaymentGatewayAdapter;
import com.payment.infrastructure.gateway.iyzico.IyzicoPaymentGatewayAdapter;
import com.payment.support.PaymentTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Katman: INTEGRATION - gateway seçimi.
 * <p>
 * İki adapter da {@code @ConditionalOnProperty} ile korunur. Yanlış yapılandırma
 * iki senaryodan birine yol açar: (a) hiç gateway bean'i olmaz ve uygulama
 * ayağa kalkmaz, (b) iki bean birden yüklenir ve PaymentGatewayPort belirsizleşir.
 * Bu test gerçek Spring context'i kurarak her iki riski de kapatır.
 */
@DisplayName("Payment Service - Integration: gateway seçimi (@ConditionalOnProperty)")
class PaymentGatewaySelectionIntegrationTest {

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackageClasses = {DevPaymentGatewayAdapter.class, IyzicoPaymentGatewayAdapter.class})
    static class GatewayScanConfiguration {
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GatewayScanConfiguration.class, RestTemplateConfig.class)
            .withConfiguration(AutoConfigurations.of());

    @Test
    @DisplayName("I28: Gateway seçimi - property hiç verilmezse dev adapter yüklenir (matchIfMissing)")
    void gatewaySelection_WhenProviderPropertyIsMissing_ShouldLoadDevAdapter() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PaymentGatewayPort.class);
            assertThat(context).hasSingleBean(DevPaymentGatewayAdapter.class);
            assertThat(context).doesNotHaveBean(IyzicoPaymentGatewayAdapter.class);
        });
    }

    @Test
    @DisplayName("I29: Gateway seçimi - provider=dev iken yalnızca dev adapter yüklenir")
    void gatewaySelection_WhenProviderIsDev_ShouldLoadOnlyDevAdapter() {
        contextRunner.withPropertyValues("payment.gateway.provider=dev").run(context -> {
            assertThat(context).hasSingleBean(PaymentGatewayPort.class);
            assertThat(context).hasSingleBean(DevPaymentGatewayAdapter.class);
            assertThat(context).doesNotHaveBean(IyzicoPaymentGatewayAdapter.class);
        });
    }

    @Test
    @DisplayName("I30: Gateway seçimi - provider=iyzico iken yalnızca iyzico adapter yüklenir")
    void gatewaySelection_WhenProviderIsIyzico_ShouldLoadOnlyIyzicoAdapter() {
        contextRunner.withPropertyValues("payment.gateway.provider=iyzico").run(context -> {
            assertThat(context).hasSingleBean(PaymentGatewayPort.class);
            assertThat(context).hasSingleBean(IyzicoPaymentGatewayAdapter.class);
            assertThat(context).doesNotHaveBean(DevPaymentGatewayAdapter.class);
        });
    }

    @Test
    @DisplayName("I31: Gateway seçimi - provider=iyzico iken her tahsilat PaymentGatewayNotConfiguredException üretir")
    void gatewaySelection_WhenProviderIsIyzico_EveryChargeShouldFailLoudly() {
        contextRunner.withPropertyValues("payment.gateway.provider=iyzico").run(context -> {
            PaymentGatewayPort gateway = context.getBean(PaymentGatewayPort.class);

            assertThatThrownBy(() -> gateway.charge(
                    PaymentTestFixtures.chargeCommand(UUID.randomUUID(), UUID.randomUUID(), "100.00")))
                    .isInstanceOf(PaymentGatewayNotConfiguredException.class);
        });
    }

    @Test
    @DisplayName("I32: Gateway seçimi - provider=dev iken tahsilat gerçekten çalışır")
    void gatewaySelection_WhenProviderIsDev_ChargeShouldSucceed() {
        contextRunner.withPropertyValues("payment.gateway.provider=dev").run(context -> {
            PaymentGatewayPort gateway = context.getBean(PaymentGatewayPort.class);

            assertThat(gateway.charge(PaymentTestFixtures.chargeCommand(
                    UUID.randomUUID(), UUID.randomUUID(), "100.00")).status())
                    .isEqualTo(PaymentStatus.COMPLETED);
        });
    }

    @Test
    @DisplayName("I33: Gateway seçimi - Bilinmeyen provider değerinde HİÇBİR gateway yüklenmez (sessiz tahsilat riski yok)")
    void gatewaySelection_WhenProviderIsUnknown_ShouldLoadNoGatewayAtAll() {
        contextRunner.withPropertyValues("payment.gateway.provider=stripe").run(context -> {
            assertThat(context).doesNotHaveBean(PaymentGatewayPort.class);
        });
    }
}
