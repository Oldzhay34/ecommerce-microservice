package com.cart.infrastructure.persistence.adapter;

import com.cart.application.port.out.CartCommandPort;
import com.cart.application.port.out.CartQueryPort;
import com.cart.domain.model.Cart;
import com.cart.infrastructure.persistence.entity.CartJpaEntity;
import com.cart.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.cart.infrastructure.persistence.mapper.CartEntityMapper;
import com.cart.infrastructure.persistence.repository.CartRepository;
import com.cart.infrastructure.persistence.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
@org.springframework.modulith.NamedInterface("infrastructure.persistence.adapter")
public class CartPersistenceAdapter implements CartCommandPort, CartQueryPort {

    private final CartRepository cartRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final CartEntityMapper cartEntityMapper;

    public CartPersistenceAdapter(CartRepository cartRepository,
                                  OutboxEventRepository outboxEventRepository,
                                  CartEntityMapper cartEntityMapper) {
        this.cartRepository = cartRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.cartEntityMapper = cartEntityMapper;
    }

    @Override
    public Cart save(Cart cart) {
        CartJpaEntity entity = cartEntityMapper.toEntity(cart);
        CartJpaEntity savedEntity = cartRepository.save(entity);
        return cartEntityMapper.toDomain(savedEntity);
    }

    @Override
    public void saveOutboxEvent(String aggregateId, String eventType, String payload) {
        OutboxEventJpaEntity outboxEvent = new OutboxEventJpaEntity();
        outboxEvent.setAggregateId(aggregateId);
        outboxEvent.setEventType(eventType);
        outboxEvent.setPayload(payload);
        outboxEventRepository.save(outboxEvent);
    }

    @Override
    public Optional<Cart> findByUserId(UUID userId) {
        return cartRepository.findByUserId(userId).map(cartEntityMapper::toDomain);
    }
}