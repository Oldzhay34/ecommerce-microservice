package com.cart.application.port.in;

import com.cart.api.dto.CartResponse;
import java.util.UUID;

@org.springframework.modulith.NamedInterface("port.in")
public interface CartQueryUseCase {
    CartResponse getCart(UUID userId);
}