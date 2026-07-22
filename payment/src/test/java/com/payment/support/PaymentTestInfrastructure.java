package com.payment.support;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * payment servisinin GERÇEKTEN kullandığı üç altyapı bileşeni:
 * <ul>
 *   <li>PostgreSQL - payments / outbox_event tabloları (yazma modeli, JPA)</li>
 *   <li>Elasticsearch - PaymentDocument okuma modeli (PaymentQueryPort buradan okur)</li>
 *   <li>RabbitMQ - order.approved dinleyicisi + outbox yayınları (payment.exchange)</li>
 * </ul>
 * <p>
 * Container'lar bilerek AYRI bir holder sınıfında tutulur: Docker yokluğunda somut
 * test sınıfları {@code @EnabledIf} ile devre dışı bırakıldığında bu sınıf hiç
 * yüklenmez, dolayısıyla static blok da çalışmaz ve "Docker yok" hatası alınmaz.
 * Container'lar tüm test sınıfları arasında paylaşılır (singleton container pattern).
 */
public final class PaymentTestInfrastructure {

    private PaymentTestInfrastructure() {
    }

    public static final PostgreSQLContainer<?> POSTGRES;
    public static final RabbitMQContainer RABBITMQ;
    public static final ElasticsearchContainer ELASTICSEARCH;

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("payment_db_test")
                .withUsername("test")
                .withPassword("test");

        RABBITMQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

        ELASTICSEARCH = new ElasticsearchContainer(
                DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.0"))
                .withEnv("xpack.security.enabled", "false")
                .withEnv("discovery.type", "single-node")
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

        POSTGRES.start();
        RABBITMQ.start();
        ELASTICSEARCH.start();
    }

    public static String elasticsearchUri() {
        return "http://" + ELASTICSEARCH.getHttpHostAddress().replace("http://", "");
    }
}
