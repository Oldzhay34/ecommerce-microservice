package com.cart.infrastructure.messaging.publisher;

import com.cart.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.cart.infrastructure.persistence.repository.OutboxEventRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@org.springframework.modulith.NamedInterface("infrastructure.messaging.publisher")
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "cart.exchange";

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Yayın periyodu property'den okunur. Subsystem/system testlerinde
     * app.outbox.publish-rate-ms çok büyük verilerek arka plan yayınlayıcısı
     * pratikte devre dışı bırakılır; böylece testin okuduğu outbox satırlarıyla
     * scheduler yarışmaz ve testler deterministik olur.
     */
    @Scheduled(fixedDelayString = "${app.outbox.publish-rate-ms:5000}")
    @Transactional
    public void publishEvents() {
        List<OutboxEventJpaEntity> unprocessedEvents = outboxEventRepository.findByProcessedFalse();

        for (OutboxEventJpaEntity event : unprocessedEvents) {
            String routingKey = "cart." + event.getEventType().toLowerCase();

            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event.getPayload());

            event.setProcessed(true);
            outboxEventRepository.save(event);
        }
    }
}