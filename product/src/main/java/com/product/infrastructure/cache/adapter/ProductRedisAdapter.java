package com.product.infrastructure.cache.adapter;

import com.product.application.port.out.ProductCachePort;
import com.product.domain.model.Product;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class ProductRedisAdapter implements ProductCachePort {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductRedisAdapter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<List<Product>> getCachedSearchResults(String cacheKey) {
        String json = redisTemplate.opsForValue().get(cacheKey);
        if (json != null) {
            try {
                List<Product> products = objectMapper.readValue(json, new TypeReference<List<Product>>() {});
                return Optional.of(products);
            } catch (JsonProcessingException e) {
                // Log and ignore cache miss on error
            }
        }
        return Optional.empty();
    }

    @Override
    public void cacheSearchResults(String cacheKey, List<Product> products, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(products);
            redisTemplate.opsForValue().set(cacheKey, json, ttlSeconds, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            // Log error
        }
    }

    @Override
    public void invalidateCache(String cachePrefix) {
        Set<String> keys = redisTemplate.keys(cachePrefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}