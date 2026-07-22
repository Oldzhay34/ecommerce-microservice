package com.mediaservice.infrastructure.cache.adapter;

import com.mediaservice.api.dto.MediaAssetResponse;
import com.mediaservice.application.port.out.MediaCachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cache-aside. Write-through YAPILMAZ: once PostgreSQL commit, sonra invalidate.
 * [Karar A] TTL degeri media.cache.ttl-minutes ile disariya alindi (varsayilan 30 dk).
 * [Karar B] Redis'te YALNIZCA metadata JSON tutulur; WebP byte[] Redis'e YAZILMAZ -
 * gorseller MinIO public URL'inden servis edilir.
 */
@Component
public class MediaRedisAdapter implements MediaCachePort {

    private static final Logger log = LoggerFactory.getLogger(MediaRedisAdapter.class);
    private static final String KEY_PREFIX = "media:product:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration ttl;

    public MediaRedisAdapter(RedisTemplate<String, Object> redisTemplate,
                             @Value("${media.cache.ttl-minutes}") long ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    private String key(UUID productId) {
        return KEY_PREFIX + productId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MediaAssetResponse> get(UUID productId) {
        try {
            Object value = redisTemplate.opsForValue().get(key(productId));
            return value == null ? null : (List<MediaAssetResponse>) value;
        } catch (Exception e) {
            log.warn("Redis okuma hatasi, DB'ye dusuluyor. productId={}", productId, e);
            return null;
        }
    }

    @Override
    public void put(UUID productId, List<MediaAssetResponse> items) {
        try {
            redisTemplate.opsForValue().set(key(productId), items, ttl);
        } catch (Exception e) {
            log.warn("Redis yazma hatasi (yok sayildi). productId={}", productId, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<UUID, List<MediaAssetResponse>> multiGet(List<UUID> productIds) {
        Map<UUID, List<MediaAssetResponse>> result = new HashMap<>();
        if (productIds == null || productIds.isEmpty()) {
            return result;
        }
        try {
            List<String> keys = new ArrayList<>(productIds.size());
            for (UUID id : productIds) {
                keys.add(key(id));
            }
            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null) {
                return result;
            }
            for (int i = 0; i < productIds.size(); i++) {
                Object value = values.get(i);
                if (value != null) {
                    result.put(productIds.get(i), (List<MediaAssetResponse>) value);
                }
            }
        } catch (Exception e) {
            log.warn("Redis multiGet hatasi, tumu DB'den okunacak.", e);
            return new HashMap<>();
        }
        return result;
    }

    @Override
    public void putAll(Map<UUID, List<MediaAssetResponse>> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        // multiSet TTL desteklemedigi icin tek tek yaziliyor (hard TTL zorunlu).
        for (Map.Entry<UUID, List<MediaAssetResponse>> entry : entries.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void invalidate(UUID productId) {
        try {
            redisTemplate.delete(key(productId));
        } catch (Exception e) {
            log.warn("Redis invalidate hatasi. productId={}", productId, e);
        }
    }
}