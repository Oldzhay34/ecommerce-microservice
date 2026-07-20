package com.promptengineering.auth.infrastructure.messaging.publisher;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class OutboxEventPublisher {

    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;

    public OutboxEventPublisher(JdbcTemplate jdbcTemplate, RabbitTemplate rabbitTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "5000")
    @Transactional
    public void publishOutboxEvents() {
        String selectQuery = "SELECT id, event_type, payload FROM outbox_event WHERE published_at IS NULL LIMIT 10";

        List<Map<String, Object>> events = jdbcTemplate.queryForList(selectQuery);

        if (!events.isEmpty()) {
            System.out.println("Scheduler çalıştı. Bulunan kayıt sayısı: " + events.size());
        }

        for (Map<String, Object> event : events) {
            try {
                UUID id = (UUID) event.get("id");
                String eventType = (String) event.get("event_type");
                String payload = (String) event.get("payload");

                // 1. DÜZELTME: String'i JSON formatında ham bir AMQP mesajına çeviriyoruz.
                Message message = MessageBuilder
                        .withBody(payload.getBytes(StandardCharsets.UTF_8))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .build();

                // 2. DÜZELTME: convertAndSend yerine doğrudan send kullanıyoruz ki converter araya girmesin.
                rabbitTemplate.send("ecommerce.topic", eventType, message);

                String updateQuery = "UPDATE outbox_event SET published_at = ? WHERE id = ?";
                jdbcTemplate.update(updateQuery, Timestamp.valueOf(LocalDateTime.now()), id);

            } catch (Exception e) {
                System.err.println("Outbox event publish failed for record. Reason: " + e.getMessage());
            }
        }
    }
}