package com.payment.unit;

import com.payment.PaymentApplication;
import com.payment.api.dto.RefundRequest;
import com.payment.infrastructure.gateway.dev.DevPaymentGatewayAdapter;
import com.payment.infrastructure.gateway.iyzico.IyzicoPaymentGatewayAdapter;
import com.payment.infrastructure.messaging.publisher.OutboxEventPublisher;
import com.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import com.payment.infrastructure.persistence.repository.PaymentRepository;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Katman: UNIT - regresyon kilitleri.
 * <p>
 * Bu sınıftaki testlerin bir kısmı payment servisinde TESPİT EDİLİP DÜZELTİLEN
 * gerçek production bug'larının geri gelmesini engeller; bir kısmı ise doğru olan
 * ama sessizce bozulabilecek yapılandırmayı sabitler. Testler zayıflatılmamalıdır.
 */
@DisplayName("UNIT - Payment konfigürasyon regresyonları (bulunan production bug'ları)")
class PaymentConfigurationRegressionTest {

    /**
     * BUG 1 (DÜZELTİLDİ): PaymentApplication'da @EnableScheduling YOKTU.
     * OutboxEventPublisher @Scheduled ile işaretli olmasına rağmen scheduling
     * altyapısı hiç kurulmadığı için ASLA tetiklenmiyordu: outbox_event satırları
     * birikiyor, RabbitMQ'ya tek bir PaymentCompleted/PaymentRefunded olayı bile
     * yayınlanmıyor, sipariş servisi ödemenin sonucunu hiç öğrenemiyordu.
     */
    @Test
    @DisplayName("U90: PaymentApplication - @EnableScheduling bulunmalı, aksi halde outbox publisher HİÇ çalışmaz")
    void paymentApplication_ShouldEnableScheduling() {
        assertThat(PaymentApplication.class.getAnnotation(EnableScheduling.class))
                .as("@EnableScheduling yoksa OutboxEventPublisher#publishOutboxEvents hiç tetiklenmez")
                .isNotNull();
    }

    /**
     * BUG 2 (DÜZELTİLDİ): @Scheduled(fixedDelay = 5000) sabitti; subsystem
     * testlerinde scheduler, testin yazıp okuduğu outbox satırlarıyla yarışıyordu.
     */
    @Test
    @DisplayName("U91: OutboxEventPublisher - Yayın periyodu property ile ayarlanabilir olmalı")
    void outboxEventPublisher_ShouldExposeConfigurablePublishRate() throws Exception {
        Method method = OutboxEventPublisher.class.getMethod("publishOutboxEvents");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.fixedDelayString()).contains("app.outbox.publish-rate-ms");
    }

    /**
     * BUG 3 (DÜZELTİLDİ): İdempotency kontrolü yoktu ve okuma modeli (Elasticsearch)
     * near-real-time olduğu için oradan yapılan bir kontrol de yetmezdi. Kontrol
     * güçlü tutarlı yazma modeli üzerinden yapılmalıdır.
     */
    @Test
    @DisplayName("U92: PaymentRepository - Idempotency sorgusu yazma modelinde tanımlı olmalı")
    void paymentRepository_ShouldExposeExistsByOrderIdOnWriteModel() throws Exception {
        Method method = PaymentRepository.class.getMethod("existsByOrderId", UUID.class);

        assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    /**
     * Gateway seçimi: dev adapter'ı varsayılan (matchIfMissing = true), iyzico
     * adapter'ı yalnızca açıkça istendiğinde yüklenir. İkisinin de aynı anda
     * yüklenmesi PaymentGatewayPort için birden çok bean hatası üretir.
     */
    @Test
    @DisplayName("U93: DevPaymentGatewayAdapter - provider belirtilmediğinde varsayılan gateway olmalı")
    void devGatewayAdapter_ShouldBeDefaultWhenProviderPropertyIsMissing() {
        ConditionalOnProperty condition = DevPaymentGatewayAdapter.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(condition).isNotNull();
        assertThat(condition.name()).containsExactly("payment.gateway.provider");
        assertThat(condition.havingValue()).isEqualTo("dev");
        assertThat(condition.matchIfMissing()).isTrue();
    }

    @Test
    @DisplayName("U94: IyzicoPaymentGatewayAdapter - Yalnızca provider=iyzico iken yüklenmeli (varsayılan DEĞİL)")
    void iyzicoGatewayAdapter_ShouldOnlyLoadWhenExplicitlySelected() {
        ConditionalOnProperty condition = IyzicoPaymentGatewayAdapter.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(condition).isNotNull();
        assertThat(condition.name()).containsExactly("payment.gateway.provider");
        assertThat(condition.havingValue()).isEqualTo("iyzico");
        assertThat(condition.matchIfMissing())
                .as("iyzico varsayılan olursa her ödeme PaymentGatewayNotConfiguredException'a düşer")
                .isFalse();
    }

    /**
     * @Pattern/@Min null'ı GEÇERLİ sayar; iade sebebi için @NotBlank kullanılmalıdır.
     * Bu servis için kontrol edildi: RefundRequest.reason zaten @NotBlank taşıyor -
     * test bunun sessizce zayıflatılmasını engeller.
     */
    @Test
    @DisplayName("U95: RefundRequest - reason null/boş gönderilirse doğrulama hatası üretmelidir")
    void refundRequest_WhenReasonIsNullOrBlank_ShouldFailValidation() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            RefundRequest missing = new RefundRequest();
            assertThat(validator.validate(missing))
                    .as("reason null iken istek 400 ile reddedilmeli, use case'de NPE'ye dönüşmemeli")
                    .isNotEmpty();

            RefundRequest blank = new RefundRequest();
            blank.setReason("   ");
            assertThat(validator.validate(blank)).isNotEmpty();

            RefundRequest valid = new RefundRequest();
            valid.setReason("ürün hasarlı");
            assertThat(validator.validate(valid)).isEmpty();
        }
    }

    /**
     * Bilinen kalıp: @GeneratedValue + uygulamanın atadığı ID, merge/StaleObjectState
     * sorunlarına yol açar. Bu serviste ID'yi VERİTABANI atıyor (uygulama hiçbir yerde
     * yeni ödemeye ID vermiyor), dolayısıyla @GeneratedValue doğru kullanımdır.
     */
    @Test
    @DisplayName("U96: PaymentJpaEntity - Kimlik veritabanı tarafından üretilmelidir")
    void paymentJpaEntity_ShouldLetDatabaseGenerateIdentifier() throws Exception {
        Field id = PaymentJpaEntity.class.getDeclaredField("id");

        assertThat(id.getAnnotation(Id.class)).isNotNull();
        assertThat(id.getAnnotation(GeneratedValue.class)).isNotNull();
    }
}
