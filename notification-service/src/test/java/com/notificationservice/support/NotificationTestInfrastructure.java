package com.notificationservice.support;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Subsystem ve System-Alpha testlerinin paylaştığı altyapı: Postgres + RabbitMQ
 * (Testcontainers) ve in-process GreenMail SMTP sunucusu.
 * <p>
 * Container'lar bilinçli olarak AYRI bir holder sınıfında tutulur: test sınıfı
 * Docker yokluğunda {@code @EnabledIf} ile devre dışı bırakıldığında bu sınıf hiç
 * yüklenmez, dolayısıyla static blok da çalışmaz. Container'lar tüm test sınıfları
 * arasında paylaşılır (singleton container pattern) ve JVM kapanınca Ryuk temizler.
 */
public final class NotificationTestInfrastructure {

    private NotificationTestInfrastructure() {}

    public static final PostgreSQLContainer<?> POSTGRES;
    public static final RabbitMQContainer RABBITMQ;
    public static final GreenMail GREENMAIL;

    /** GreenMail'in dinlediği SMTP portu; sabit tutulur ki Spring property'si deterministik olsun. */
    private static final int SMTP_PORT = 3125;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("notification_db_test")
                .withUsername("test")
                .withPassword("test");

        RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management");

        POSTGRES.start();
        RABBITMQ.start();

        GREENMAIL = new GreenMail(new ServerSetup(SMTP_PORT, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
        GREENMAIL.start();
    }

    public static int smtpPort() {
        return GREENMAIL.getSmtp().getPort();
    }

    /** Testler arası izolasyon: önceki testin mailleri sonraki testi kirletmesin. */
    public static void resetMailbox() {
        GREENMAIL.reset();
    }
}
