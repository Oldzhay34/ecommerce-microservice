package com.notificationservice.application.usecase;

import com.notificationservice.application.port.in.SendOtpEmailUseCase;
import com.notificationservice.application.port.out.EmailSenderPort;
import com.notificationservice.application.port.out.ProcessedEventPort;
import com.notificationservice.domain.exception.DuplicateEventException;
import com.notificationservice.domain.model.OtpEmailPayload;
import com.notificationservice.domain.model.ProcessedEvent;
import com.notificationservice.domain.model.ProcessedEventStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SendOtpEmailUseCaseImpl implements SendOtpEmailUseCase {

    private final ProcessedEventPort processedEventPort;
    private final EmailSenderPort emailSenderPort;

    public SendOtpEmailUseCaseImpl(ProcessedEventPort processedEventPort, EmailSenderPort emailSenderPort) {
        this.processedEventPort = processedEventPort;
        this.emailSenderPort = emailSenderPort;
    }

    @Override
    public void sendOtpEmail(String idempotencyKey, OtpEmailPayload payload) {
        if (processedEventPort.isEventProcessed(idempotencyKey)) {
            throw new DuplicateEventException("Event already processed: " + idempotencyKey);
        }

        emailSenderPort.sendOtpEmail(payload);

        ProcessedEvent event = new ProcessedEvent(
                idempotencyKey,
                "auth.event.otp",
                ProcessedEventStatus.SENT,
                LocalDateTime.now()
        );
        processedEventPort.save(event);
    }
}