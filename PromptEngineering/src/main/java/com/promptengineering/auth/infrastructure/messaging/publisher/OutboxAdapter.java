package com.promptengineering.auth.infrastructure.messaging.publisher;

import com.promptengineering.auth.application.port.out.OutboxPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class OutboxAdapter implements OutboxPort {

    private final JdbcTemplate jdbcTemplate;

    public OutboxAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void publishNotificationEvent(String email, String message) {
        // GÜNCELLEME: Veritabanı Entity (OutboxJpaEvent) yapısı ile birebir uyumlu sorgu
        // id, event_type, payload, created_at kolonları eşleştirildi.
        String sql = "INSERT INTO outbox_event (id, event_type, payload, created_at) VALUES (?, ?, ?, ?)";

        String payload = String.format("{\"email\":\"%s\", \"otpCode\":\"%s\"}", email, message);

        jdbcTemplate.update(sql,
                UUID.randomUUID(),
                "auth.event.otp",
                payload,
                Timestamp.valueOf(LocalDateTime.now()));
    }
}