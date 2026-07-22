package com.cart.subsystem;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Katman: SUBSYSTEM - cart servisinin GERÇEKTEN kullandığı üç altyapı bileşeni
 * container'da ayağa kaldırılır:
 *   - PostgreSQL  : carts / cart_items / outbox_event tabloları (JPA)
 *   - RabbitMQ    : outbox yayınları (cart.exchange) + OrderCreatedEvent listener'ı
 *   - Redis       : CartRedisAdapter (cart:user:{id} anahtarları)
 *
 * NOT: Bu sınıf cart servisine ÖZELDİR; başka bir servisle paylaşılan bir test
 * modülü yoktur. Redis için Testcontainers'ın resmi bir modülü olmadığından
 * çekirdek GenericContainer kullanılır (ekstra bağımlılık gerekmez).
 *
 * ÇALIŞTIRMA ÖNKOŞULU: Docker. Docker olmayan makinelerde bu hiyerarşideki
 * testler koşturulamaz (CI'da ubuntu-latest runner'ında Docker hazırdır).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractCartSubsystemTest {

    static final PostgreSQLContainer<?> POSTGRES;
    static final RabbitMQContainer RABBITMQ;
    static final GenericContainer<?> REDIS;

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("cart_db_test")
                .withUsername("test")
                .withPassword("test");

        RABBITMQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);

        POSTGRES.start();
        RABBITMQ.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        // Testlerde JWT secret'ını sabitliyoruz ki testler kendi token'larını üretebilsin.
        registry.add("jwt.secret", () -> "EcommerceSharedJwtSecretKey2026VeryLongAndSecure32Bytes");

        // Arka plan outbox yayınlayıcısını pratikte KAPAT: @Scheduled publisher,
        // testin okuduğu/yazdığı outbox satırlarıyla yarışıyor. Testlerde
        // publisher elle tetiklenir, böylece deterministik çalışır.
        registry.add("app.outbox.publish-rate-ms", () -> "3600000");
    }
}
