package com.cart.systemalpha;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Katman: SYSTEM ALPHA - TAM STACK, BLACK-BOX.
 *
 * Bu hiyerarşideki testler uygulamaya SADECE public HTTP API'den girer.
 * Repository / use case / RabbitTemplate gibi uygulama bean'leri KASITLI
 * OLARAK inject EDİLMEZ; doğrulama yalnızca HTTP yanıtları veya RabbitMQ
 * kuyruğu üzerinden yapılır (kuyruğa da uygulamanın bean'i ile değil,
 * container bilgisinden kurulan ham AMQP istemcisiyle bağlanılır).
 *
 * Subsystem katmanından farklı olarak burada outbox scheduler'ı KAPATILMAZ:
 * gerçek kullanıcı akışında olayların kendiliğinden yayınlanması da
 * sözleşmenin parçasıdır.
 *
 * NOT: Bu sınıf cart servisine özeldir; subsystem katmanının base sınıfından
 * bilerek ayrıdır, çünkü property'leri (scheduler açık) ve amacı farklıdır.
 *
 * ÇALIŞTIRMA ÖNKOŞULU: Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public abstract class AbstractCartSystemAlphaTest {

    protected static final String JWT_SECRET = "EcommerceSharedJwtSecretKey2026VeryLongAndSecure32Bytes";

    protected static final PostgreSQLContainer<?> POSTGRES;
    protected static final RabbitMQContainer RABBITMQ;
    protected static final GenericContainer<?> REDIS;

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("cart_db_e2e")
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
        registry.add("jwt.secret", () -> JWT_SECRET);

        // E2E'de scheduler AÇIK kalır: olayların kendiliğinden yayınlanması da
        // doğrulanan davranışın parçasıdır.
        registry.add("app.outbox.publish-rate-ms", () -> "1000");
    }

    /**
     * Gerçek bir istemcinin göndereceği gibi imzalı JWT üretir. Uygulamanın
     * hiçbir bean'ine dokunmaz - sadece paylaşılan secret'ı kullanır.
     */
    protected static String bearerToken(UUID userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        String jwt = Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of("role", role))
                .issuedAt(new Date(now - 1000))
                .expiration(new Date(now + 900_000))
                .signWith(key)
                .compact();
        return "Bearer " + jwt;
    }

    protected static HttpHeaders jsonHeaders(UUID userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, bearerToken(userId, role));
        return headers;
    }
}
