package com.review.infrastructure.messaging.publisher;

import com.review.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.review.infrastructure.persistence.repository.OutboxEventRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@EnableScheduling
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, RabbitTemplate rabbitTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    // Testlerde scheduler'ın outbox satırlarıyla yarışmaması için periyot
    // dışarıdan ayarlanabilir hale getirildi (subsystem testleri publisher'ı
    // deterministik olsun diye elle tetikler).
    @Scheduled(fixedDelayString = "${app.outbox.publish-rate-ms:5000}")
    @Transactional
    public void publishEvents() {
        List<OutboxEventJpaEntity> events = outboxEventRepository.findAll();
        for (OutboxEventJpaEntity event : events) {
            rabbitTemplate.convertAndSend("review.exchange", event.getType(), event.getPayload());
            outboxEventRepository.delete(event);
        }
    }
}