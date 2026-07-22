package com.notificationservice.infrastructure.messaging.listener;

import com.notificationservice.application.port.in.SendOtpEmailUseCase;
import com.notificationservice.domain.exception.DuplicateEventException;
import com.notificationservice.domain.model.OtpEmailPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class OtpEmailEventListener {

    private static final Logger log = LoggerFactory.getLogger(OtpEmailEventListener.class);
    private final SendOtpEmailUseCase sendOtpEmailUseCase;

    public OtpEmailEventListener(SendOtpEmailUseCase sendOtpEmailUseCase) {
        this.sendOtpEmailUseCase = sendOtpEmailUseCase;
    }

    @RabbitListener(queues = "${rabbitmq.queue.otp}")
    public void handleOtpEvent(@Payload OtpEmailPayload payload,
                               @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {

        // Fallback: Eğer Outbox publisher messageId göndermediyse, email+otpCode üzerinden hash üretilir.
        String idempotencyKey = (messageId != null && !messageId.trim().isEmpty())
                ? messageId + "-" + payload.getEmail()
                : generateFallbackHash(payload.getEmail(), payload.getOtpCode());

        try {
            sendOtpEmailUseCase.sendOtpEmail(idempotencyKey, payload);
            log.info("OTP email successfully sent and event processed. Key: {}", idempotencyKey);
        } catch (DuplicateEventException e) {
            log.info("Event already processed, silently acknowledging. Key: {}", idempotencyKey);
        }
        // EmailSendException fırlatıldığında burada yutulmaz, RabbitMQ retry/DLQ mekanizması tetiklenir.
    }

    private String generateFallbackHash(String email, String otpCode) {
        try {
            String raw = email + ":" + otpCode;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not found", e);
        }
    }
}