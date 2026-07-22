package com.cart.api.controller;

import com.cart.api.dto.AddToCartRequest;
import com.cart.api.dto.UpdateCartItemRequest;
import com.cart.api.dto.CartResponse;
import com.cart.application.port.in.CartCommandUseCase;
import com.cart.application.port.in.CartQueryUseCase;
import com.cart.domain.exception.UnauthorizedCartAccessException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID; // EKLENDİ

@RestController
@RequestMapping("/api/carts")
@org.springframework.modulith.NamedInterface("controller")
public class CartController {

    private final CartCommandUseCase cartCommandUseCase;
    private final CartQueryUseCase cartQueryUseCase;

    public CartController(CartCommandUseCase cartCommandUseCase, CartQueryUseCase cartQueryUseCase) {
        this.cartCommandUseCase = cartCommandUseCase;
        this.cartQueryUseCase = cartQueryUseCase;
    }

    @PostMapping("/{userId}/items")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Void> addItem(@PathVariable UUID userId, @Valid @RequestBody AddToCartRequest request) {
        validateIdor(userId);
        cartCommandUseCase.addItemToCart(userId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}/items/{productId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Void> updateItem(@PathVariable UUID userId, @PathVariable UUID productId, @Valid @RequestBody UpdateCartItemRequest request) { // Long -> UUID
        validateIdor(userId);
        cartCommandUseCase.updateCartItemQuantity(userId, productId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/items/{productId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Void> removeItem(@PathVariable UUID userId, @PathVariable UUID productId) { // Long -> UUID
        validateIdor(userId);
        cartCommandUseCase.removeCartItem(userId, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Void> clearCart(@PathVariable UUID userId) {
        validateIdor(userId);
        cartCommandUseCase.clearCart(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<CartResponse> getCart(@PathVariable UUID userId) {
        validateIdor(userId);
        return ResponseEntity.ok(cartQueryUseCase.getCart(userId));
    }

    private void validateIdor(UUID targetUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserIdStr = (String) auth.getPrincipal();
        UUID currentUserId = UUID.fromString(currentUserIdStr); // UUID'ye çevrildi

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!currentUserId.equals(targetUserId) && !isAdmin) {
            throw new UnauthorizedCartAccessException("IDOR VULNERABILITY PREVENTED: You cannot access another user's cart.");
        }
    }
}