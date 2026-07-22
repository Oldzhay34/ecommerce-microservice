package com.cart.unit;

import com.cart.domain.model.Cart;
import com.cart.infrastructure.cache.adapter.CartRedisAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT. RedisTemplate mock'lanır; hedef cache HIT/MISS dalları,
 * anahtar formatı, TTL ve null guard'lar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - CartRedisAdapter")
class CartRedisAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CartRedisAdapter adapter;
    private UUID userId;

    @BeforeEach
    void setUp() {
        adapter = new CartRedisAdapter(redisTemplate);
        userId = UUID.randomUUID();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("U52: getCartByUserId - Cache HIT durumunda dolu Optional ve doğru anahtar kullanılır")
    void getCartByUserId_WhenCacheHit_ShouldReturnCartUsingPrefixedKey() {
        Cart cached = new Cart();
        cached.setUserId(userId);
        when(valueOperations.get("cart:user:" + userId)).thenReturn(cached);

        assertThat(adapter.getCartByUserId(userId)).containsSame(cached);
    }

    @Test
    @DisplayName("U53: getCartByUserId - Cache MISS durumunda boş Optional döner")
    void getCartByUserId_WhenCacheMiss_ShouldReturnEmptyOptional() {
        when(valueOperations.get("cart:user:" + userId)).thenReturn(null);

        assertThat(adapter.getCartByUserId(userId)).isEmpty();
    }

    @Test
    @DisplayName("U54: saveCart - Sepet 24 saatlik TTL ile doğru anahtara yazılır")
    void saveCart_WhenCartValid_ShouldWriteWithTwentyFourHourTtl() {
        Cart cart = new Cart();
        cart.setUserId(userId);

        adapter.saveCart(cart);

        verify(valueOperations).set("cart:user:" + userId, cart, Duration.ofHours(24));
    }

    @Test
    @DisplayName("U55: saveCart - Sepet null ise Redis'e hiç dokunulmaz")
    void saveCart_WhenCartIsNull_ShouldDoNothing() {
        adapter.saveCart(null);

        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("U56: saveCart - Sepetin userId'si null ise yazma yapılmaz (anahtar üretilemez)")
    void saveCart_WhenUserIdIsNull_ShouldDoNothing() {
        adapter.saveCart(new Cart());

        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("U57: invalidateCache - Kullanıcının cache anahtarı silinir")
    void invalidateCache_ShouldDeletePrefixedKey() {
        adapter.invalidateCache(userId);

        verify(redisTemplate).delete("cart:user:" + userId);
        verifyNoInteractions(valueOperations);
    }
}
