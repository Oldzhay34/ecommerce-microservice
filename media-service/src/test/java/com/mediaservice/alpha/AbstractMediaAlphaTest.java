package com.mediaservice.alpha;

import com.mediaservice.infrastructure.config.RabbitMqConfig;
import com.mediaservice.support.JwtTestTokens;
import com.mediaservice.support.MediaTestInfrastructure;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Katman: ALPHA - TAM STACK, BLACK-BOX, TEK UZUN YOLCULUK senaryosu.
 * <p>
 * system katmanindan farki: system her testte TEK bir ozelligi dogrularken, alpha
 * gercek bir kullanicinin/magazanin baslangictan sona kadar yapacagi ADIM ADIM
 * yolculugu (upload -> siralama -> kapak degistirme -> silme -> urun silindiginde
 * kaskad -> outbox'in gercekten yayinlanmasi) TEK bir surekli anlatida dogrular
 * (bkz. payment/PaymentJourneySystemAlphaTest).
 * <p>
 * Uygulama bean'leri KASITLI OLARAK inject edilmez. RabbitMQ'ya bile uygulamanin
 * bean'iyle degil, container koordinatlarindan kurulan BAGIMSIZ bir AMQP istemcisiyle
 * baglanilir - hem product-service'in product.deleted olayini yayinlarken, hem de
 * media-service'in media.exchange'e gercekten bastigini dogrularken.
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractMediaAlphaTest {

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

    protected static HttpHeaders authHeaders(UUID userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, JwtTestTokens.bearer(JwtTestTokens.tokenWithRole(userId, role)));
        return headers;
    }

    private static Connection openConnection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(MediaTestInfrastructure.RABBITMQ.getHost());
        factory.setPort(MediaTestInfrastructure.RABBITMQ.getAmqpPort());
        factory.setUsername("guest");
        factory.setPassword("guest");
        return factory.newConnection();
    }

    /**
     * product-service'in yerine gecerek product.deleted olayini yayinlar. Uygulamanin
     * RabbitTemplate bean'i DEGIL, container koordinatlarindan kurulan ham AMQP
     * istemcisi kullanilir (bkz. class javadoc'u).
     */
    protected static void publishProductDeletedEvent(String payload) throws Exception {
        try (Connection connection = openConnection(); Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(RabbitMqConfig.PRODUCT_EXCHANGE, "topic", true);
            channel.basicPublish(RabbitMqConfig.PRODUCT_EXCHANGE, RabbitMqConfig.RK_PRODUCT_DELETED, null,
                    payload.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * media.exchange'e baglanan bagimsiz, kalici bir kuyruk acar ve BAGLI kalir - taahhut
     * edilen olay HENUZ yayinlanmadan once cagrilmalidir (aksi halde topic exchange'in
     * dogasi geregi henuz kimsenin dinlemedigi mesaj kaybolur). Donen {@link ListenerHandle}
     * daha sonra {@link #awaitMessage} ile GERCEKTEN broker'a dusen mesaji bekler.
     */
    protected static ListenerHandle bindQueue(String routingKey) throws Exception {
        Connection connection = openConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(RabbitMqConfig.MEDIA_EXCHANGE, "topic", true);
        String queue = channel.queueDeclare().getQueue();
        channel.queueBind(queue, RabbitMqConfig.MEDIA_EXCHANGE, routingKey);
        return new ListenerHandle(connection, channel, queue);
    }

    protected static String awaitMessage(ListenerHandle handle, long timeoutMillis) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        try {
            while (System.currentTimeMillis() < deadline) {
                var response = handle.channel.basicGet(handle.queue, true);
                if (response != null) {
                    return new String(response.getBody(), StandardCharsets.UTF_8);
                }
                Thread.sleep(200);
            }
            throw new AssertionError("media.exchange kuyrugunda mesaj gelmedi (timeout)");
        } finally {
            handle.channel.close();
            handle.connection.close();
        }
    }

    protected record ListenerHandle(Connection connection, Channel channel, String queue) {
    }
}
