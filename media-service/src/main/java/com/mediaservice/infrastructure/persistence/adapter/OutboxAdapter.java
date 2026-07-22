package com.mediaservice.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediaservice.application.port.out.OutboxPort;
import com.mediaservice.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.mediaservice.infrastructure.persistence.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxAdapter implements OutboxPort {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxAdapter(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * MANDATORY: cagiran use case'in transaction'i ZORUNLUDUR. Boylece media_asset yazimi ile
     * outbox_event yazimi atomik olur (Transactional Outbox Pattern).
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String aggregateType,
                       UUID aggregateId,
                       String eventType,
                       String routingKey,
                       Object payload) {
        OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setAggregateType(aggregateType);
        entity.setAggregateId(aggregateId);
        entity.setEventType(eventType);
        entity.setRoutingKey(routingKey);
        entity.setPayload(serialize(payload));
        entity.setProcessed(false);
        entity.setCreatedAt(Instant.now());
        repository.save(entity);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox payload serilestirilemedi: " + eventTypeOf(payload), e);
        }
    }

    private String eventTypeOf(Object payload) {
        return payload == null ? "null" : payload.getClass().getSimpleName();
    }
}