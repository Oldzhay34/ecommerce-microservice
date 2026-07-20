package com.order.infrastructure.messaging.publisher;

import com.order.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.order.infrastructure.persistence.repository.OutboxEventRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "order.exchange";

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEventJpaEntity> unprocessedEvents = outboxEventRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEventJpaEntity event : unprocessedEvents) {
            String routingKey = getRoutingKey(event.getEventType());
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event.getPayload());

            event.setProcessed(true);
            outboxEventRepository.save(event);
        }
    }

    private String getRoutingKey(String eventType) {
        return switch (eventType) {
            case "OrderCreatedEvent" -> "order.created";
            case "OrderApprovedEvent" -> "order.approved";
            case "OrderCancelledEvent" -> "order.cancelled";
            case "OrderShippedEvent" -> "order.shipped";
            default -> "order.generic";
        };
    }
}