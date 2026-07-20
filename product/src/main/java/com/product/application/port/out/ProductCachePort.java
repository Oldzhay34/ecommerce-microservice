package com.product.application.port.out;

import com.product.domain.model.Product;
import java.util.List;
import java.util.Optional;

@org.springframework.modulith.NamedInterface("port.out")
public interface ProductCachePort {
    Optional<List<Product>> getCachedSearchResults(String cacheKey);
    void cacheSearchResults(String cacheKey, List<Product> products, long ttlSeconds);
    void invalidateCache(String cachePrefix);
}