package com.notificationservice.subsystem;

import com.icegreen.greenmail.util.GreenMail;
import com.notificationservice.support.NotificationTestInfrastructure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Subsystem test tabanı: uygulamanın TÜM bean'leri gerçek altyapıyla ayağa kalkar.
 * <p>
 * Postgres + RabbitMQ Testcontainers ile, SMTP ise in-process GreenMail ile sağlanır.
 * Docker olmayan makinelerde tüm alt sınıflar SKIP edilir.
 */
@SpringBootTest(properties = {
        // application.yaml'daki ${SMTP_USER}/${SMTP_PASS} placeholder'larını ez:
        // testte kimlik doğrulamasız GreenMail kullanıyoruz.
        "spring.mail.username=",
        "spring.mail.password=",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "spring.mail.properties.mail.smtp.starttls.required=false",
        "spring.sql.init.mode=always"
})
@EnabledIf("com.notificationservice.support.DockerAvailability#isDockerAvailable")
public abstract class AbstractNotificationSubsystemTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

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

    protected GreenMail greenMail() {
        return NotificationTestInfrastructure.GREENMAIL;
    }

    @BeforeEach
    void resetSharedState() {
        NotificationTestInfrastructure.resetMailbox();
        jdbcTemplate.execute("TRUNCATE TABLE processed_event");
    }

    protected int processedEventCount(String idempotencyKey) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM processed_event WHERE idempotency_key = ?", Integer.class, idempotencyKey);
        return count == null ? 0 : count;
    }
}
