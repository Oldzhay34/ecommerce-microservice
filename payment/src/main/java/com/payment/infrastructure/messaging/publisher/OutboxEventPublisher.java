package com.payment.infrastructure.messaging.publisher;

import com.payment.infrastructure.messaging.RabbitMQConfig;
import com.payment.infrastructure.persistence.entity.OutboxJpaEntity;
import com.payment.infrastructure.persistence.repository.OutboxRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxEventPublisher {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxEventPublisher(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    // Sabit fixedDelay = 5000 idi; subsystem testlerinde scheduler, testin okuyup
    // yazdığı outbox satırlarıyla yarışıyordu. Periyot artık property ile
    // ayarlanabilir (varsayılan davranış değişmedi: 5000 ms).
    @Scheduled(fixedDelayString = "${app.outbox.publish-rate-ms:5000}")
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxJpaEntity> events = outboxRepository.findAll();
        for (OutboxJpaEntity event : events) {
            String routingKey = determineRoutingKey(event.getType());
            rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, routingKey, event.getPayload());
            outboxRepository.delete(event);
        }
    }

    private String determineRoutingKey(String eventType) {
        if ("PaymentCompletedEvent".equals(eventType)) return "payment.completed";
        if ("PaymentFailedEvent".equals(eventType)) return "payment.failed";
        if ("PaymentRefundedEvent".equals(eventType)) return "payment.refunded";
        return "payment.unknown";
    }
}