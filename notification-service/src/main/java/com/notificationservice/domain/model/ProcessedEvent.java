package com.notificationservice.domain.model;

import java.time.LocalDateTime;

public class ProcessedEvent {
    private String idempotencyKey;
    private String eventType;
    private ProcessedEventStatus status;
    private LocalDateTime createdAt;

    public ProcessedEvent() {}

    public ProcessedEvent(String idempotencyKey, String eventType, ProcessedEventStatus status, LocalDateTime createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.eventType = eventType;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public ProcessedEventStatus getStatus() { return status; }
    public void setStatus(ProcessedEventStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}