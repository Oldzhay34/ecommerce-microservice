package com.review.systemalpha;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * System-Alpha test tabanı: TAM STACK, BLACK-BOX.
 *
 * Kurallar (bilinçli olarak subsystem katmanından daha katıdır):
 *  - Uygulamanın hiçbir bean'i (repository, adapter, use case, publisher)
 *    inject EDİLMEZ.
 *  - Sisteme YALNIZCA public HTTP API'den girilir.
 *  - Doğrulama yalnızca (a) public HTTP API yanıtları veya (b) RabbitMQ
 *    kuyrukları üzerinden yapılır.
 *  - RabbitMQ erişimi bile uygulamanın RabbitTemplate bean'i ile değil,
 *    container koordinatlarından kurulan BAĞIMSIZ bir istemci ile yapılır -
 *    böylece test, dışarıdaki bir tüketici/üretici gibi davranır.
 *
 * NOT: Docker gerektirir; Docker'sız makinede çalıştırılamaz.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractReviewSystemAlphaTest {

    /** Uygulamanın application.yaml varsayılanıyla aynı paylaşılan secret. */
    protected static final String JWT_SECRET =
            "EcommerceSharedJwtSecretKey2026VeryLongAndSecure32Bytes";

    protected static final String ORDER_EXCHANGE = "order.exchange";
    protected static final String ORDER_SHIPPED_ROUTING_KEY = "order.shipped";
    protected static final String REVIEW_EXCHANGE = "review.exchange";

    static final PostgreSQLContainer<?> POSTGRES;
    static final RabbitMQContainer RABBITMQ;
    static final ElasticsearchContainer ELASTICSEARCH;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("review_db_e2e")
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
        registry.add("spring.docker.compose.enabled", () -> "false");

        // Subsystem katmanının aksine outbox publisher AÇIK bırakılır: black-box
        // testte publisher'ı elle tetikleyemeyiz (bean inject yasak), event'in
        // kuyruğa gerçekten kendi kendine düşmesini beklemek zorundayız.
        registry.add("app.outbox.publish-rate-ms", () -> "500");
    }

    /**
     * Uygulamanın bean'lerinden TAMAMEN bağımsız, dışarıdan bağlanan bir
     * RabbitMQ istemcisi. Her testte kendi bağlantısını açar.
     */
    protected static RabbitTemplate externalRabbitClient() {
        return new RabbitTemplate(externalConnectionFactory());
    }

    protected static RabbitAdmin externalRabbitAdmin() {
        return new RabbitAdmin(externalConnectionFactory());
    }

    private static CachingConnectionFactory externalConnectionFactory() {
        CachingConnectionFactory factory =
                new CachingConnectionFactory(RABBITMQ.getHost(), RABBITMQ.getAmqpPort());
        factory.setUsername("guest");
        factory.setPassword("guest");
        return factory;
    }
}
