package com.mediaservice.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * media-service'in GERCEKTEN kullandigi dort altyapi bileseni:
 * <ul>
 *   <li>PostgreSQL - media_asset / outbox_event tablolari (yazma modeli, JPA + Flyway)</li>
 *   <li>RabbitMQ - media.exchange yayini + product.exchange/product.deleted dinleyicisi</li>
 *   <li>Redis - MediaQueryUseCaseImpl'in cache-aside okuma modeli (MediaCachePort)</li>
 *   <li>MinIO - WebP varyantlarinin yazildigi object storage (StoragePort)</li>
 * </ul>
 * <p>
 * Container'lar bilerek AYRI bir holder sinifinda tutulur: Docker yoklugunda somut
 * test siniflari {@code @EnabledIf} ile devre disi birakildiginda bu sinif hic
 * yuklenmez, dolayisiyla static blok da calismaz ve "Docker yok" hatasi alinmaz.
 * Container'lar tum test siniflari arasinda paylasilir (singleton container pattern,
 * bkz. payment/src/test/.../PaymentTestInfrastructure.java).
 */
public final class MediaTestInfrastructure {

    private MediaTestInfrastructure() {
    }

    public static final PostgreSQLContainer<?> POSTGRES;
    public static final RabbitMQContainer RABBITMQ;
    public static final GenericContainer<?> REDIS;
    public static final GenericContainer<?> MINIO;

    public static final String MINIO_ROOT_USER = "minioadmin";
    public static final String MINIO_ROOT_PASSWORD = "minioadmin123";
    public static final String MINIO_BUCKET = "shopbridge-media";

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("shopbridge_media_test")
                .withUsername("media_user")
                .withPassword("media_pass");

        RABBITMQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .waitingFor(Wait.forListeningPort());

        MINIO = new GenericContainer<>(DockerImageName.parse("minio/minio:RELEASE.2024-10-13T13-34-11Z"))
                .withExposedPorts(9000, 9001)
                .withEnv("MINIO_ROOT_USER", MINIO_ROOT_USER)
                .withEnv("MINIO_ROOT_PASSWORD", MINIO_ROOT_PASSWORD)
                .withCommand("server", "/data", "--console-address", ":9001")
                .waitingFor(Wait.forHttp("/minio/health/live")
                        .forPort(9000)
                        .withStartupTimeout(Duration.ofSeconds(60)));

        // Tum container'lari baslat
        POSTGRES.start();
        RABBITMQ.start();
        REDIS.start();
        MINIO.start();
    }

    public static String redisHost() {
        return REDIS.getHost();
    }

    public static Integer redisPort() {
        return REDIS.getMappedPort(6379);
    }

    public static String minioEndpoint() {
        return "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
    }
}
