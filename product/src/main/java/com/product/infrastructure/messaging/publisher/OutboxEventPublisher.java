package com.product.infrastructure.messaging.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;

    public OutboxEventPublisher(JdbcTemplate jdbcTemplate, RabbitTemplate rabbitTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "5000")
    @Transactional
    public void publishOutboxEvents() {
        String selectQuery = "SELECT id, event_type, payload FROM outbox_event WHERE published_at IS NULL " +
                "ORDER BY created_at ASC LIMIT 100 FOR UPDATE SKIP LOCKED";

        List<Map<String, Object>> events = jdbcTemplate.queryForList(selectQuery);
        log.info(">>> OUTBOX PUBLISHER çalıştı, bekleyen event sayısı: {}", events.size());

        for (Map<String, Object> event : events) {
            try {
                UUID id = (UUID) event.get("id");
                String eventType = (String) event.get("event_type");

                // jsonb kolonu PGobject olarak döner; toString() JSON metnini verir
                String payload = String.valueOf(event.get("payload"));

                rabbitTemplate.convertAndSend("ecommerce.topic", eventType, payload);

                String updateQuery = "UPDATE outbox_event SET published_at = ? WHERE id = ?";
                jdbcTemplate.update(updateQuery, Timestamp.valueOf(LocalDateTime.now()), id);

                log.info(">>> Event yayınlandı: {} (id={})", eventType, id);

            } catch (Exception e) {
                log.error(">>> OUTBOX HATA: {}", e.getMessage(), e);
            }
        }
    }
}