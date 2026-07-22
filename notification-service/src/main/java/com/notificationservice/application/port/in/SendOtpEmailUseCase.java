package com.notificationservice.application.port.in;

import com.notificationservice.domain.model.OtpEmailPayload;

public interface SendOtpEmailUseCase {
    void sendOtpEmail(String idempotencyKey, OtpEmailPayload payload);
}