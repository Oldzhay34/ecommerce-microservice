package com.mediaservice.system;

import com.mediaservice.support.JwtTestTokens;
import com.mediaservice.support.MediaTestInfrastructure;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

/**
 * Katman: SYSTEM - GERCEK altyapi (Postgres + RabbitMQ + Redis + MinIO), ancak
 * uygulamaya YALNIZCA public HTTP API'den girilir. Repository / use case bean'leri
 * KASITLI OLARAK inject edilmez; tek dogrulama noktasi {@code TestRestTemplate}'tir.
 * <p>
 * subsystem katmanindan farki: subsystem beyaz kutudur (bean inject edilir, DB
 * dogrudan sorgulanir); system tamamen kara kutudur - servis disaridan bir istemci
 * gibi test edilir.
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b> {@code @EnabledIf} {@code @Inherited} olmadigi
 * icin her somut alt sinifa ayri ayri yazilir.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractMediaSystemTest {

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

        registry.add("app.security.jwt-secret", () -> JwtTestTokens.SECRET);
    }

    /**
     * Gercek bir istemcinin gonderecegi gibi imzali JWT uretir; uygulamanin hicbir
     * bean'ine dokunmaz, yalnizca paylasilan secret'i kullanir.
     */
    protected static HttpHeaders authHeaders(UUID userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, JwtTestTokens.bearer(JwtTestTokens.tokenWithRole(userId, role)));
        return headers;
    }
}
