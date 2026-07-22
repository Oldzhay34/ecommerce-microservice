package com.notificationservice.application.port.out;

import com.notificationservice.domain.model.OtpEmailPayload;

public interface EmailSenderPort {
    void sendOtpEmail(OtpEmailPayload payload);
}