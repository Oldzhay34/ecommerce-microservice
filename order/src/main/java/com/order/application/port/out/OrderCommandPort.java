package com.order.application.port.out;

import com.order.domain.model.Order;

@org.springframework.modulith.NamedInterface("port.out")
public interface OrderCommandPort {
    Order save(Order order);
    void saveOutboxEvent(String aggregateId, String aggregateType, String eventType, String payload);
}