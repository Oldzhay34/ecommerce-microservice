package com.order.infrastructure.persistence.mapper;

import com.order.domain.model.Order;
import com.order.domain.model.OrderItem;
import com.order.infrastructure.persistence.entity.OrderJpaEntity;
import com.order.infrastructure.persistence.entity.OrderItemJpaEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class OrderEntityMapper {

    public OrderJpaEntity toEntity(Order domain) {
        if (domain == null) return null;

        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setStatus(domain.getStatus());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setCreatedAt(domain.getCreatedAt());

        if (domain.getItems() != null) {
            for (OrderItem itemDomain : domain.getItems()) {
                OrderItemJpaEntity itemEntity = new OrderItemJpaEntity();
                itemEntity.setId(itemDomain.getId());
                itemEntity.setProductId(itemDomain.getProductId());
                itemEntity.setStoreId(itemDomain.getStoreId());
                itemEntity.setQuantity(itemDomain.getQuantity());
                itemEntity.setPrice(itemDomain.getPrice());
                entity.addItem(itemEntity);
            }
        }
        return entity;
    }

    public Order toDomain(OrderJpaEntity entity) {
        if (entity == null) return null;

        Order domain = new Order();
        domain.setId(entity.getId());
        domain.setUserId(entity.getUserId());
        domain.setStatus(entity.getStatus());
        domain.setTotalAmount(entity.getTotalAmount());
        domain.setCreatedAt(entity.getCreatedAt());

        if (entity.getItems() != null) {
            domain.setItems(entity.getItems().stream().map(itemEntity -> {
                OrderItem itemDomain = new OrderItem();
                itemDomain.setId(itemEntity.getId());
                itemDomain.setProductId(itemEntity.getProductId());
                itemDomain.setStoreId(itemEntity.getStoreId());
                itemDomain.setQuantity(itemEntity.getQuantity());
                itemDomain.setPrice(itemEntity.getPrice());
                return itemDomain;
            }).collect(Collectors.toList()));
        }
        return domain;
    }
}