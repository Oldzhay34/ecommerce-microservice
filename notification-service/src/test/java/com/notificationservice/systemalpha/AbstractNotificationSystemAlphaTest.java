package com.notificationservice.systemalpha;

import com.icegreen.greenmail.util.GreenMail;
import com.notificationservice.support.NotificationTestInfrastructure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;

/**
 * System-Alpha (black-box) test tabanı.
 * <p>
 * Sistemin sınırları: GİRİŞ = RabbitMQ kuyruğu, ÇIKIŞ = SMTP posta kutusu.
 * Bu katmanda hiçbir uygulama bean'i (listener, usecase, adapter) inject EDİLMEZ;
 * yalnızca dış arayüzler kullanılır. Docker yoksa testler SKIP edilir.
 */
@SpringBootTest(properties = {
        "spring.mail.username=",
        "spring.mail.password=",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "spring.mail.properties.mail.smtp.starttls.required=false",
        "spring.sql.init.mode=always"
})
@EnabledIf("com.notificationservice.support.DockerAvailability#isDockerAvailable")
public abstract class AbstractNotificationSystemAlphaTest {

    protected static final String EXCHANGE = "ecommerce.topic";
    protected static final String ROUTING_KEY = "auth.event.otp";
    protected static final String DLQ = "notification.email.otp.dlq";

    /** Sadece dış arayüz: mesaj yayınlamak için transport. */
    @Autowired
    protected RabbitTemplate rabbitTemplate;

    /** Sadece test izolasyonu için; assertion'larda kullanılmaz. */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", NotificationTestInfrastructure.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", NotificationTestInfrastructure.POSTGRES::getUsername);
        registry.add("spring.datasource.password", NotificationTestInfrastructure.POSTGRES::getPassword);

        registry.add("spring.rabbitmq.host", NotificationTestInfrastructure.RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", NotificationTestInfrastructure.RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");

        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", NotificationTestInfrastructure::smtpPort);
    }

    @BeforeEach
    void resetSharedState() {
        NotificationTestInfrastructure.resetMailbox();
        jdbcTemplate.execute("TRUNCATE TABLE processed_event");
        while (rabbitTemplate.receive(DLQ, 100) != null) {
            // önceki testlerden kalan DLQ mesajlarını temizle
        }
    }

    protected GreenMail mailbox() {
        return NotificationTestInfrastructure.GREENMAIL;
    }

    /**
     * Auth servisinin yayınlayacağı gibi ham JSON mesaj üretir: yalnızca
     * contentType=application/json ve messageId; Java tip bilgisi (__TypeId__) YOK.
     */
    protected Message otpEventMessage(String json, String messageId) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        if (messageId != null) {
            properties.setMessageId(messageId);
        }
        return MessageBuilder
                .withBody(json.getBytes(StandardCharsets.UTF_8))
                .andProperties(properties)
                .build();
    }

    protected void publishOtpEvent(String json, String messageId) {
        rabbitTemplate.send(EXCHANGE, ROUTING_KEY, otpEventMessage(json, messageId));
    }
}
