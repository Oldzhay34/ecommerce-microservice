package com.order.infrastructure.persistence.adapter;

import com.order.application.port.out.OrderCommandPort;
import com.order.domain.model.Order;
import com.order.infrastructure.persistence.entity.OrderJpaEntity;
import com.order.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.order.infrastructure.persistence.mapper.OrderEntityMapper;
import com.order.infrastructure.persistence.repository.OrderRepository;
import com.order.infrastructure.persistence.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OrderPersistenceAdapter implements OrderCommandPort {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderEntityMapper orderEntityMapper;

    public OrderPersistenceAdapter(OrderRepository orderRepository,
                                   OutboxEventRepository outboxEventRepository,
                                   OrderEntityMapper orderEntityMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.orderEntityMapper = orderEntityMapper;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = orderEntityMapper.toEntity(order);
        OrderJpaEntity savedEntity = orderRepository.save(entity);
        return orderEntityMapper.toDomain(savedEntity);
    }

    @Override
    public void saveOutboxEvent(String aggregateId, String aggregateType, String eventType, String payload) {
        OutboxEventJpaEntity outbox = new OutboxEventJpaEntity();
        outbox.setAggregateId(aggregateId);
        outbox.setAggregateType(aggregateType);
        outbox.setEventType(eventType);
        outbox.setPayload(payload);
        outbox.setCreatedAt(LocalDateTime.now());
        outbox.setProcessed(false);
        outboxEventRepository.save(outbox);
    }
}