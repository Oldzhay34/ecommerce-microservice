package com.order.subsystem;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Subsystem testler için ortak altyapı: gerçek Postgres, RabbitMQ ve
 * Elasticsearch container'ları. Container'lar sınıf bazında (static) tek
 * sefer ayağa kalkar ve tüm subsystem test sınıfları arasında paylaşılır
 * (Testcontainers'ın JVM ömrü boyunca yeniden kullanımı - reuse - CI'da
 * build süresini kısaltmak için önemlidir).
 *
 * NOT: Bu katmanda product servisi YOKTUR. Order servisinin kendi
 * altyapısıyla (DB + Rabbit + ES) tutarlı çalıştığını kanıtlıyoruz.
 * Stok onay/red event'leri gerektiğinde testin kendisi tarafından
 * simüle edilir (bkz. OrderCreationSubsystemTest).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractOrderSubsystemTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("orderdb_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management");

    @Container
    static final ElasticsearchContainer ELASTICSEARCH =
            new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.0"))
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("discovery.type", "single-node");

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

        // Testte scheduled job'ı beklerken gereksiz gürültü olmasın diye
        // ddl-auto update kalsın (Flyway/Liquibase yoksa mevcut davranışla tutarlı).
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }
}