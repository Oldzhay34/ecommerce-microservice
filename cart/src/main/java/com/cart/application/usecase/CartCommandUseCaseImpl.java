package com.cart.application.usecase;

import com.cart.api.dto.AddToCartRequest;
import com.cart.api.dto.UpdateCartItemRequest;
import com.cart.application.port.in.CartCommandUseCase;
import com.cart.application.port.out.CartCachePort;
import com.cart.application.port.out.CartCommandPort;
import com.cart.application.port.out.CartQueryPort;
import com.cart.domain.model.Cart;
import com.cart.domain.model.CartItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@org.springframework.modulith.NamedInterface("usecase")
public class CartCommandUseCaseImpl implements CartCommandUseCase {

    private final CartCommandPort cartCommandPort;
    private final CartQueryPort cartQueryPort;
    private final CartCachePort cartCachePort;
    private final ObjectMapper objectMapper;

    public CartCommandUseCaseImpl(CartCommandPort cartCommandPort,
                                  CartQueryPort cartQueryPort,
                                  CartCachePort cartCachePort,
                                  ObjectMapper objectMapper) {
        this.cartCommandPort = cartCommandPort;
        this.cartQueryPort = cartQueryPort;
        this.cartCachePort = cartCachePort;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void addItemToCart(UUID userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cart.getItems();
        boolean itemExists = false;

        for (CartItem item : items) {
            // DÜZELTME BURADA: UUID kıyaslaması yapılıyor
            if (item.getProductId().equals(request.getProductId())) {
                item.setQuantity(item.getQuantity() + request.getQuantity());
                itemExists = true;
                break;
            }
        }

        if (!itemExists) {
            items.add(new CartItem(null, request.getProductId(), request.getQuantity(), request.getPrice()));
        }

        recalculateTotalAmount(cart);

        cart = cartCommandPort.save(cart);
        publishEvent(userId, "CartUpdatedEvent", cart);
        cartCachePort.invalidateCache(userId);
    }

    @Override
    @Transactional
    // DÜZELTME BURADA: Long productId -> UUID productId
    public void updateCartItemQuantity(UUID userId, UUID productId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        cart.getItems().removeIf(item -> {
            if (item.getProductId().equals(productId)) {
                if (request.getQuantity() <= 0) {
                    return true;
                }
                item.setQuantity(request.getQuantity());
            }
            return false;
        });

        recalculateTotalAmount(cart);

        cart = cartCommandPort.save(cart);
        publishEvent(userId, "CartUpdatedEvent", cart);
        cartCachePort.invalidateCache(userId);
    }

    @Override
    @Transactional
    // DÜZELTME BURADA: Long productId -> UUID productId
    public void removeCartItem(UUID userId, UUID productId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));

        recalculateTotalAmount(cart);

        cart = cartCommandPort.save(cart);
        publishEvent(userId, "CartUpdatedEvent", cart);
        cartCachePort.invalidateCache(userId);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        cartQueryPort.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cart.setTotalAmount(BigDecimal.ZERO);

            cartCommandPort.save(cart);
            publishEvent(userId, "CartClearedEvent", cart);
            cartCachePort.invalidateCache(userId);
        });
    }

    private Cart getOrCreateCart(UUID userId) {
        return cartQueryPort.findByUserId(userId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUserId(userId);
            return newCart;
        });
    }

    private void recalculateTotalAmount(Cart cart) {
        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(total);
    }

    private void publishEvent(UUID userId, String eventType, Cart cart) {
        try {
            String payload = objectMapper.writeValueAsString(cart);
            cartCommandPort.saveOutboxEvent(userId.toString(), eventType, payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Event serialization failed", e);
        }
    }
}