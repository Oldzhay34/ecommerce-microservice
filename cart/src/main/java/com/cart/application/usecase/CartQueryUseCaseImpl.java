package com.cart.application.usecase;

import com.cart.api.dto.CartItemResponse;
import com.cart.api.dto.CartResponse;
import com.cart.application.port.in.CartQueryUseCase;
import com.cart.application.port.out.CartCachePort;
import com.cart.application.port.out.CartQueryPort;
import com.cart.domain.model.Cart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@org.springframework.modulith.NamedInterface("usecase")
public class CartQueryUseCaseImpl implements CartQueryUseCase {

    private final CartQueryPort cartQueryPort;
    private final CartCachePort cartCachePort;

    public CartQueryUseCaseImpl(CartQueryPort cartQueryPort, CartCachePort cartCachePort) {
        this.cartQueryPort = cartQueryPort;
        this.cartCachePort = cartCachePort;
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        Cart cart = cartCachePort.getCartByUserId(userId).orElseGet(() -> {
            Cart dbCart = cartQueryPort.findByUserId(userId).orElseGet(() -> {
                Cart newCart = new Cart();
                newCart.setUserId(userId);
                return newCart;
            });
            cartCachePort.saveCart(dbCart);
            return dbCart;
        });

        return mapToResponse(cart);
    }

    private CartResponse mapToResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setUserId(cart.getUserId());
        response.setTotalAmount(cart.getTotalAmount());
        if (cart.getItems() != null) {
            response.setItems(cart.getItems().stream().map(item -> {
                CartItemResponse itemRes = new CartItemResponse();
                itemRes.setProductId(item.getProductId());
                itemRes.setQuantity(item.getQuantity());
                itemRes.setPrice(item.getPrice());
                return itemRes;
            }).collect(Collectors.toList()));
        }
        return response;
    }
}