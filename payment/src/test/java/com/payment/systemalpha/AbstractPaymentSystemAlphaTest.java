package com.payment.systemalpha;

import com.payment.support.JwtTestTokens;
import com.payment.support.PaymentTestInfrastructure;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Katman: SYSTEM ALPHA - TAM STACK, BLACK-BOX.
 * <p>
 * Bu hiyerarşideki testler uygulamaya <b>SADECE public HTTP API'den</b> girer.
 * Repository / use case / RabbitTemplate gibi UYGULAMA BEAN'LERİ KASITLI OLARAK
 * INJECT EDİLMEZ. RabbitMQ'ya bile uygulamanın bean'iyle değil, container
 * koordinatlarından kurulan BAĞIMSIZ bir AMQP istemcisiyle bağlanılır - tıpkı
 * gerçekte olayı yayınlayan order servisi gibi.
 * <p>
 * Subsystem katmanından farkı: burada outbox scheduler KAPATILMAZ; olayların
 * kendiliğinden yayınlanması da sözleşmenin parçasıdır.
 * <p>
 * <b>ÇALIŞTIRMA ÖNKOŞULU: Docker.</b> {@code @EnabledIf} {@code @Inherited} olmadığı
 * için Docker koşulu her somut alt sınıfa ayrı ayrı yazılır.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public abstract class AbstractPaymentSystemAlphaTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PaymentTestInfrastructure.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", PaymentTestInfrastructure.POSTGRES::getUsername);
        registry.add("spring.datasource.password", PaymentTestInfrastructure.POSTGRES::getPassword);

        registry.add("spring.rabbitmq.host", PaymentTestInfrastructure.RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", PaymentTestInfrastructure.RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");

        registry.add("spring.elasticsearch.uris", PaymentTestInfrastructure::elasticsearchUri);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("jwt.secret", () -> JwtTestTokens.SECRET);
        registry.add("payment.gateway.provider", () -> "dev");

        // E2E'de scheduler AÇIK kalır: olayların kendiliğinden yayınlanması da
        // doğrulanan davranışın parçasıdır.
        registry.add("app.outbox.publish-rate-ms", () -> "1000");
    }

    /**
     * Gerçek bir istemcinin göndereceği gibi imzalı JWT üretir; uygulamanın hiçbir
     * bean'ine dokunmaz, yalnızca paylaşılan secret'ı kullanır.
     */
    protected static HttpHeaders headers(UUID userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION,
                JwtTestTokens.bearer(JwtTestTokens.token(userId.toString(), role)));
        return headers;
    }

    /**
     * order servisinin yerine geçerek order.approved olayını yayınlar. Uygulamanın
     * RabbitTemplate bean'i DEĞİL, container koordinatlarından kurulan ham AMQP
     * istemcisi kullanılır.
     */
    protected static void publishOrderApprovedEvent(String payload) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(PaymentTestInfrastructure.RABBITMQ.getHost());
        factory.setPort(PaymentTestInfrastructure.RABBITMQ.getAmqpPort());
        factory.setUsername("guest");
        factory.setPassword("guest");

        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            channel.exchangeDeclare("order.exchange", "topic", true);
            channel.basicPublish("order.exchange", "order.approved", null,
                    payload.getBytes(StandardCharsets.UTF_8));
        }
    }
}
