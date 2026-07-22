package com.cart.infrastructure.persistence.mapper;

import com.cart.domain.model.Cart;
import com.cart.domain.model.CartItem;
import com.cart.infrastructure.persistence.entity.CartItemJpaEntity;
import com.cart.infrastructure.persistence.entity.CartJpaEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@org.springframework.modulith.NamedInterface("infrastructure.mapper")
public class CartEntityMapper {

    public Cart toDomain(CartJpaEntity entity) {
        if (entity == null) return null;

        Cart cart = new Cart();
        cart.setId(entity.getId());
        cart.setUserId(entity.getUserId());
        cart.setTotalAmount(entity.getTotalAmount());

        if (entity.getItems() != null) {
            cart.setItems(entity.getItems().stream().map(this::toDomainItem).collect(Collectors.toList()));
        }
        return cart;
    }

    private CartItem toDomainItem(CartItemJpaEntity entity) {
        CartItem item = new CartItem();
        item.setId(entity.getId());
        item.setProductId(entity.getProductId());
        item.setQuantity(entity.getQuantity());
        item.setPrice(entity.getPrice());
        return item;
    }

    public CartJpaEntity toEntity(Cart domain) {
        if (domain == null) return null;

        CartJpaEntity entity = new CartJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setTotalAmount(domain.getTotalAmount());

        if (domain.getItems() != null) {
            domain.getItems().forEach(item -> {
                CartItemJpaEntity itemEntity = toEntityItem(item);
                entity.addItem(itemEntity);
            });
        }
        return entity;
    }

    private CartItemJpaEntity toEntityItem(CartItem domain) {
        CartItemJpaEntity entity = new CartItemJpaEntity();
        entity.setId(domain.getId());
        entity.setProductId(domain.getProductId());
        entity.setQuantity(domain.getQuantity());
        entity.setPrice(domain.getPrice());
        return entity;
    }
}