package com.cart.infrastructure.cache.adapter;

import com.cart.application.port.out.CartCachePort;
import com.cart.domain.model.Cart;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@org.springframework.modulith.NamedInterface("infrastructure.cache.adapter")
public class CartRedisAdapter implements CartCachePort {

    private static final String CART_KEY_PREFIX = "cart:user:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;

    public CartRedisAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<Cart> getCartByUserId(UUID userId) {
        Cart cart = (Cart) redisTemplate.opsForValue().get(CART_KEY_PREFIX + userId.toString());
        return Optional.ofNullable(cart);
    }

    @Override
    public void saveCart(Cart cart) {
        if (cart != null && cart.getUserId() != null) {
            redisTemplate.opsForValue().set(CART_KEY_PREFIX + cart.getUserId().toString(), cart, TTL);
        }
    }

    @Override
    public void invalidateCache(UUID userId) {
        redisTemplate.delete(CART_KEY_PREFIX + userId.toString());
    }
}