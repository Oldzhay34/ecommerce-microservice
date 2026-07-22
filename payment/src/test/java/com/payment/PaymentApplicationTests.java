package com.payment;

import com.payment.subsystem.AbstractPaymentSubsystemTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Çıplak {@code @SpringBootTest} ile context yüklenemiyordu: payment servisi ayağa
 * kalkarken PostgreSQL, RabbitMQ ve Elasticsearch bağlantılarına ihtiyaç duyar. Bu
 * yüzden (order/cart/review servislerindeki kalıpla aynı şekilde) test
 * container'larını sağlayan {@link AbstractPaymentSubsystemTest}'ten türetilmiştir.
 *
 * <p><b>ÇALIŞTIRMA ÖNKOŞULU: Docker.</b> {@code @EnabledIf} {@code @Inherited}
 * olmadığı için koşul burada, somut sınıfta tanımlıdır.
 */
@EnabledIf("com.payment.support.DockerAvailability#isDockerAvailable")
@DisplayName("SUBSYSTEM - PaymentApplication context yüklenmesi")
class PaymentApplicationTests extends AbstractPaymentSubsystemTest {

    @Test
    @DisplayName("S11: contextLoads - Tüm bean grafiği gerçek altyapıyla ayağa kalkar")
    void contextLoads() {
    }
}
