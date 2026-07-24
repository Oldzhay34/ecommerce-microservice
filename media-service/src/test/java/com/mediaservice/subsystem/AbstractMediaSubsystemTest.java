package com.mediaservice.subsystem;

import com.mediaservice.support.JwtTestTokens;
import com.mediaservice.support.MediaTestInfrastructure;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Katman: SUBSYSTEM - media-service'in GERCEKTEN kullandigi dort altyapi bileseni
 * container'da ayaga kaldirilir:
 * <ul>
 *   <li>PostgreSQL : media_asset / outbox_event tablolari (Flyway ile olusturulur)</li>
 *   <li>RabbitMQ   : product.deleted dinleyicisi + media.exchange yayinlari</li>
 *   <li>Redis      : MediaQueryUseCaseImpl'in cache-aside okuma modeli</li>
 *   <li>MinIO      : WebP varyantlarinin yazildigi object storage</li>
 * </ul>
 * <p>
 * Bu katmanda uygulama bean'lerini inject etmek SERBESTTIR (beyaz kutu): DB / cache /
 * storage / outbox durumu dogrudan dogrulanabilir.
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b> {@code @EnabledIf} {@code @Inherited} OLMADIGI
 * icin Docker kosulu bu base sinifa DEGIL, her somut alt sinifa ayri ayri yazilir
 * (payment servisindeki desenle birebir aynidir).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractMediaSubsystemTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MediaTestInfrastructure.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", MediaTestInfrastructure.POSTGRES::getUsername);
        registry.add("spring.datasource.password", MediaTestInfrastructure.POSTGRES::getPassword);

        registry.add("spring.rabbitmq.host", MediaTestInfrastructure.RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", MediaTestInfrastructure.RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");

        registry.add("spring.data.redis.host", MediaTestInfrastructure::redisHost);
        registry.add("spring.data.redis.port", MediaTestInfrastructure::redisPort);

        registry.add("minio.endpoint", MediaTestInfrastructure::minioEndpoint);
        registry.add("minio.access-key", () -> MediaTestInfrastructure.MINIO_ROOT_USER);
        registry.add("minio.secret-key", () -> MediaTestInfrastructure.MINIO_ROOT_PASSWORD);
        registry.add("minio.bucket", () -> MediaTestInfrastructure.MINIO_BUCKET);
        registry.add("media.public-base-url",
                () -> MediaTestInfrastructure.minioEndpoint() + "/" + MediaTestInfrastructure.MINIO_BUCKET);

        // Testler kendi imzali token'larini uretebilsin diye secret sabitlenir.
        registry.add("app.security.jwt-secret", () -> JwtTestTokens.SECRET);
    }
}
