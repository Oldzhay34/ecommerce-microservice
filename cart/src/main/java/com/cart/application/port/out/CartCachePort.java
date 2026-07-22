package com.cart.application.port.out;

import com.cart.domain.model.Cart;
import java.util.Optional;
import java.util.UUID;

@org.springframework.modulith.NamedInterface("port.out")
public interface CartCachePort {
    Optional<Cart> getCartByUserId(UUID userId);
    void saveCart(Cart cart);
    void invalidateCache(UUID userId);
}