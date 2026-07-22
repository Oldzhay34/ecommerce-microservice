package com.notificationservice.application.port.out;

import com.notificationservice.domain.model.ProcessedEvent;

public interface ProcessedEventPort {
    boolean isEventProcessed(String idempotencyKey);
    void save(ProcessedEvent event);
}