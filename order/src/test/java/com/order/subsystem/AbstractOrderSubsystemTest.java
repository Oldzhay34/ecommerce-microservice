package com.order.subsystem;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractOrderSubsystemTest {

    static final PostgreSQLContainer<?> POSTGRES;
    static final RabbitMQContainer RABBITMQ;
    static final ElasticsearchContainer ELASTICSEARCH;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("orderdb_test")
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

        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");

        registry.add("spring.elasticsearch.uris",
                () -> "http://" + ELASTICSEARCH.getHttpHostAddress());

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        // Otomatik scheduler'ı pratikte KAPAT: @Scheduled outbox publisher,
        // testin oluşturduğu/okuduğu outbox satırlarıyla yarışıyordu. Testte
        // publisher'ı manuel tetikliyoruz, böylece deterministik çalışır.
        // NOT: "spring.task.scheduling.enabled" diye bir Spring Boot property'si
        // YOKTUR; eskiden burada o kullanıldığı için scheduler aslında hiç
        // kapanmıyordu.
        registry.add("app.outbox.publish-rate-ms", () -> "3600000");
    }
}