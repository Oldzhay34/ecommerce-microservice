package com.review.subsystem;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * review servisinin subsystem test tabanı. Servise ÖZELDİR - başka hiçbir
 * servisle paylaşılan ortak bir test modülü kullanılmaz.
 *
 * review-service üç gerçek altyapıya bağımlıdır ve üçü de container olarak
 * ayağa kaldırılır:
 *  - Postgres      : write model (reviews, purchase_eligibility, outbox_event)
 *  - Elasticsearch : CQRS read model (ReviewDocument / "reviews" indeksi)
 *  - RabbitMQ      : order.exchange (order.shipped tüketimi) +
 *                    review.exchange (outbox yayını)
 *
 * Container'lar static blokta bir kez başlatılır; aynı JVM içindeki tüm
 * subsystem testleri onları paylaşır.
 *
 * ÖNEMLİ: Bu testler Docker gerektirir. Docker'ın bulunmadığı bir makinede
 * çalıştırılamazlar (CI'daki ubuntu-latest runner'da Docker hazırdır).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractReviewSubsystemTest {

    protected static final String JWT_SECRET =
            "EcommerceSharedJwtSecretKey2026VeryLongAndSecure32Bytes";

    static final PostgreSQLContainer<?> POSTGRES;
    static final RabbitMQContainer RABBITMQ;
    static final ElasticsearchContainer ELASTICSEARCH;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("review_db_test")
                .withUsername("test")
                .withPassword("test");

        RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management");

        ELASTICSEARCH = new ElasticsearchContainer(
                DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.0.2"))
                .withEnv("xpack.security.enabled", "false")
                .withEnv("discovery.type", "single-node");

        POSTGRES.start();
        RABBITMQ.start();
        ELASTICSEARCH.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");

        registry.add("spring.elasticsearch.uris",
                () -> "http://" + ELASTICSEARCH.getHttpHostAddress());

        registry.add("jwt.secret", () -> JWT_SECRET);

        // Testcontainers zaten altyapıyı sağlıyor; compose.yaml'ı ayağa
        // kaldırmaya çalışan spring-boot-docker-compose devre dışı bırakılır.
        registry.add("spring.docker.compose.enabled", () -> "false");

        // @Scheduled outbox publisher'ı pratikte KAPAT: testin okuduğu/yazdığı
        // outbox satırlarıyla yarışıyor ve testleri non-deterministik yapıyor.
        // Publisher subsystem testlerinde ELLE tetiklenir.
        // (OutboxEventPublisher bu property'yi fixedDelayString ile okur.)
        registry.add("app.outbox.publish-rate-ms", () -> "3600000");
    }
}
