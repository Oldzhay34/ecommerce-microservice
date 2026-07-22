package com.cart.application.port.out;

import com.cart.domain.model.Cart;
import java.util.Optional;
import java.util.UUID;

@org.springframework.modulith.NamedInterface("port.out")
public interface CartQueryPort {
    Optional<Cart> findByUserId(UUID userId);
}