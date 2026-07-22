package com.mediaservice.infrastructure.messaging.publisher;

import com.mediaservice.infrastructure.config.RabbitMqConfig;
import com.mediaservice.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.mediaservice.infrastructure.persistence.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Transactional Outbox Pattern'in yayin ayagi.
 *
 * Servisler RabbitMQ'ya DOGRUDAN mesaj atamaz. Once PostgreSQL'deki outbox_event tablosuna
 * yazilir; bu sinif tabloyu @Scheduled ile okuyup RabbitMQ'ya basar.
 *
 * FOR UPDATE SKIP LOCKED: birden fazla publisher instance'i ayni satiri iki kez basmaz.
 * idx_outbox_event_unprocessed (processed, created_at) indeksi kullanilir.
 *
 * At-least-once teslimat: basildiktan sonra processed = true yapilirken hata olursa mesaj
 * tekrar basilabilir. Tuketiciler eventId ile idempotent olmalidir (PRODUCT SERVICE
 * ENTEGRASYON NOTU'nda belirtildi).
 */
@Component
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);
    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository,
                                RabbitTemplate rabbitTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * processed = false olanlari createdAt ASC sirayla okur (max 100), basar, processed = true yapar.
     * Tum islem tek transaction icindedir; SKIP LOCKED kilidi commit'e kadar tutulur.
     */
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventJpaEntity> pending = outboxEventRepository.lockUnprocessedBatch(BATCH_SIZE);
        if (pending.isEmpty()) {
            return;
        }

        List<UUID> publishedIds = new ArrayList<>(pending.size());

        for (OutboxEventJpaEntity event : pending) {
            try {
                rabbitTemplate.send(
                        RabbitMqConfig.MEDIA_EXCHANGE,
                        event.getRoutingKey(),
                        buildMessage(event));
                publishedIds.add(event.getId());
            } catch (Exception e) {
                // Basilamayan satir processed = false kalir; bir sonraki turda tekrar denenir.
                log.error("Outbox event basilamadi. eventId={}, routingKey={}",
                        event.getId(), event.getRoutingKey(), e);
            }
        }

        if (!publishedIds.isEmpty()) {
            outboxEventRepository.markProcessed(publishedIds, Instant.now());
            log.debug("Outbox event basildi. adet={}", publishedIds.size());
        }
    }

    /**
     * Payload zaten JSON String olarak saklandigi icin ham byte olarak basilir;
     * converter'in tekrar sarmalamasi (double encoding) onlenir.
     */
    private Message buildMessage(OutboxEventJpaEntity event) {
        return MessageBuilder
                .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(event.getId().toString())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setHeader("eventType", event.getEventType())
                .setHeader("aggregateType", event.getAggregateType())
                .setHeader("aggregateId", event.getAggregateId().toString())
                .build();
    }
}