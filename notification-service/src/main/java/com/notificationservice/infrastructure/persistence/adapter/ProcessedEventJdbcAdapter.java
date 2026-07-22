package com.notificationservice.infrastructure.persistence.adapter;

import com.notificationservice.application.port.out.ProcessedEventPort;
import com.notificationservice.domain.model.ProcessedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProcessedEventJdbcAdapter implements ProcessedEventPort {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedEventJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean isEventProcessed(String idempotencyKey) {
        String sql = "SELECT COUNT(1) FROM processed_event WHERE idempotency_key = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, idempotencyKey);
        return count != null && count > 0;
    }

    @Override
    public void save(ProcessedEvent event) {
        String sql = "INSERT INTO processed_event (idempotency_key, event_type, status, created_at) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                event.getIdempotencyKey(),
                event.getEventType(),
                event.getStatus().name(),
                event.getCreatedAt());
    }
}