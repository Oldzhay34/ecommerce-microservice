package com.cart.application.port.in;

import com.cart.api.dto.AddToCartRequest;
import com.cart.api.dto.UpdateCartItemRequest;
import java.util.UUID;

@org.springframework.modulith.NamedInterface("port.in")
public interface CartCommandUseCase {
    void addItemToCart(UUID userId, AddToCartRequest request);

    // DÜZELTME: Long productId -> UUID productId
    void updateCartItemQuantity(UUID userId, UUID productId, UpdateCartItemRequest request);

    // DÜZELTME: Long productId -> UUID productId
    void removeCartItem(UUID userId, UUID productId);

    void clearCart(UUID userId);
}