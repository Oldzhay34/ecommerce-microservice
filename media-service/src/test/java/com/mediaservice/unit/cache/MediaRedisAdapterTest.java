package com.mediaservice.unit.cache;

import com.mediaservice.api.dto.MediaAssetResponse;
import com.mediaservice.infrastructure.cache.adapter.MediaRedisAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT - RedisTemplate mock'lanir. Redis hatalari HICBIR ZAMAN caller'a
 * PROPAGATE OLMAMALI (DB'ye graceful fallback) - bu sinifin en kritik davranisi.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - MediaRedisAdapter")
class MediaRedisAdapterTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    private MediaRedisAdapter adapter;
    private UUID productId;

    @BeforeEach
    void setUp() {
        adapter = new MediaRedisAdapter(redisTemplate, 30);
        productId = UUID.randomUUID();
    }

    @Test
    @DisplayName("U1: get - Hit durumunda deger doner")
    void get_WhenHit_ShouldReturnCachedValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        List<MediaAssetResponse> cached = List.of();
        when(valueOperations.get("media:product:" + productId)).thenReturn(cached);

        assertThat(adapter.get(productId)).isSameAs(cached);
    }

    @Test
    @DisplayName("U2: get - Redis hata firlatirsa null doner, exception PROPAGATE ETMEZ")
    void get_WhenRedisThrows_ShouldReturnNullNotThrow() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        assertThat(adapter.get(productId)).isNull();
    }

    @Test
    @DisplayName("U3: put - TTL ile birlikte dogru anahtara yazar")
    void put_ShouldWriteWithConfiguredTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        List<MediaAssetResponse> items = List.of();

        adapter.put(productId, items);

        verify(valueOperations).set("media:product:" + productId, items, Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("U4: put - Redis hata firlatirsa YUTULUR, caller'a yansimaz")
    void put_WhenRedisThrows_ShouldSwallowException() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        org.assertj.core.api.Assertions.assertThatCode(() -> adapter.put(productId, List.of()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("U5: multiGet - Bos/null liste icin Redis'e HIC gidilmez")
    void multiGet_WhenEmptyOrNull_ShouldSkipRedis() {
        assertThat(adapter.multiGet(List.of())).isEmpty();
        assertThat(adapter.multiGet(null)).isEmpty();
    }

    @Test
    @DisplayName("U6: multiGet - Kismi hit'leri dogru productId'lerle esler")
    void multiGet_ShouldMapHitsByIndexToProductId() {
        UUID hitId = UUID.randomUUID();
        UUID missId = UUID.randomUUID();
        List<MediaAssetResponse> hitValue = List.of();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(any())).thenReturn(java.util.Arrays.asList(hitValue, null));

        Map<UUID, List<MediaAssetResponse>> result = adapter.multiGet(List.of(hitId, missId));

        assertThat(result).containsOnlyKeys(hitId);
        assertThat(result.get(hitId)).isSameAs(hitValue);
    }

    @Test
    @DisplayName("U7: multiGet - Redis hata firlatirsa bos map doner (tumu DB'den okunur)")
    void multiGet_WhenRedisThrows_ShouldReturnEmptyMap() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        assertThat(adapter.multiGet(List.of(productId))).isEmpty();
    }

    @Test
    @DisplayName("U8: putAll - Her entry icin ayri set cagirir (multiSet TTL desteklemedigi icin)")
    void putAll_ShouldSetEachEntrySeparately() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UUID id2 = UUID.randomUUID();

        adapter.putAll(Map.of(productId, List.of(), id2, List.of()));

        verify(valueOperations).set(eq("media:product:" + productId), any(), eq(Duration.ofMinutes(30)));
        verify(valueOperations).set(eq("media:product:" + id2), any(), eq(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("U9: invalidate - Anahtari siler; Redis hatasi yutulur")
    void invalidate_ShouldDeleteKeyAndSwallowErrors() {
        adapter.invalidate(productId);
        verify(redisTemplate).delete("media:product:" + productId);

        doThrow(new RuntimeException("redis down")).when(redisTemplate).delete(anyString());
        org.assertj.core.api.Assertions.assertThatCode(() -> adapter.invalidate(productId))
                .doesNotThrowAnyException();
    }
}
