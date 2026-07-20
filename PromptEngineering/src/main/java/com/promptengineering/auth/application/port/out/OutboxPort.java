package com.promptengineering.auth.application.port.out;

public interface OutboxPort {
    void publishNotificationEvent(String email, String message);
}