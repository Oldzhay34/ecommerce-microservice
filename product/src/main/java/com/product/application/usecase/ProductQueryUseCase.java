package com.product.application.usecase;

import com.product.application.port.out.ProductCachePort;
import com.product.application.port.out.ProductQueryPort;
import com.product.domain.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.modulith.NamedInterface("usecase")
@Service
public class ProductQueryUseCase implements com.product.application.port.in.ProductQueryUseCase {

    private final ProductQueryPort queryPort;
    private final ProductCachePort cachePort;

    public ProductQueryUseCase(ProductQueryPort queryPort, ProductCachePort cachePort) {
        this.queryPort = queryPort;
        this.cachePort = cachePort;
    }

    @Override
    public List<Product> search(String keyword, String category) {
        String cacheKey = "cache:catalog:search:" + (keyword != null ? keyword : "all") + ":" + (category != null ? category : "all");

        Optional<List<Product>> cachedResult = cachePort.getCachedSearchResults(cacheKey);
        if (cachedResult.isPresent()) {
            return cachedResult.get();
        }

        List<Product> products = queryPort.searchProducts(keyword, category);

        cachePort.cacheSearchResults(cacheKey, products, 7200L); // 2 saat TTL
        return products;
    }

    @Override
    public Product findById(UUID id) {
        // Eğer tekil ürün için de cache kullanıyorsanız buraya ekleyebilirsiniz,
        // şimdilik doğrudan veritabanı portuna (queryPort) gidiyoruz.
        return queryPort.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }
}