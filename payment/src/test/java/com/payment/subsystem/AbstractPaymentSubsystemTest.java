package com.payment.subsystem;

import com.payment.support.JwtTestTokens;
import com.payment.support.PaymentTestInfrastructure;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Katman: SUBSYSTEM - payment servisinin GERÇEKTEN kullandığı üç altyapı bileşeni
 * container'da ayağa kaldırılır:
 * <ul>
 *   <li>PostgreSQL   : payments / outbox_event tabloları (yazma modeli)</li>
 *   <li>Elasticsearch: PaymentDocument okuma modeli (PaymentQueryPort buradan okur)</li>
 *   <li>RabbitMQ     : order.approved dinleyicisi + payment.exchange yayınları</li>
 * </ul>
 * <p>
 * NOT: Bu sınıf payment servisine ÖZELDİR; başka bir servisle paylaşılan test modülü
 * yoktur. Bu katmanda uygulama bean'lerini inject etmek SERBESTTİR (beyaz kutu):
 * DB / outbox / ES durumu doğrudan doğrulanabilir.
 * <p>
 * <b>ÇALIŞTIRMA ÖNKOŞULU: Docker.</b> {@code @EnabledIf} {@code @Inherited} OLMADIĞI
 * için Docker koşulu bu base sınıfa DEĞİL, her somut alt sınıfa ayrı ayrı yazılır.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractPaymentSubsystemTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PaymentTestInfrastructure.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", PaymentTestInfrastructure.POSTGRES::getUsername);
        registry.add("spring.datasource.password", PaymentTestInfrastructure.POSTGRES::getPassword);

        registry.add("spring.rabbitmq.host", PaymentTestInfrastructure.RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", PaymentTestInfrastructure.RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");

        registry.add("spring.elasticsearch.uris", PaymentTestInfrastructure::elasticsearchUri);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        // Testler kendi imzalı token'larını üretebilsin diye secret sabitlenir.
        registry.add("jwt.secret", () -> JwtTestTokens.SECRET);

        // Varsayılan (simülasyon) gateway; harici bir sağlayıcıya çıkılmaz.
        registry.add("payment.gateway.provider", () -> "dev");

        // Arka plan outbox yayınlayıcısını pratikte KAPAT: @Scheduled publisher,
        // testin okuyup yazdığı outbox satırlarıyla yarışıyor. Bu katmanda
        // publisher elle tetiklenir, böylece doğrulama deterministik olur.
        registry.add("app.outbox.publish-rate-ms", () -> "3600000");
    }
}
