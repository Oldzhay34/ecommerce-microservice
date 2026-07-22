package com.cart.application.port.out;

import com.cart.domain.model.Cart;

@org.springframework.modulith.NamedInterface("port.out")
public interface CartCommandPort {
    Cart save(Cart cart);
    void saveOutboxEvent(String aggregateId, String eventType, String payload);
}