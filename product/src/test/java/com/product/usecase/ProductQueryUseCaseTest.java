package com.product.usecase;

import com.product.application.port.out.ProductCachePort;
import com.product.application.port.out.ProductQueryPort;
import com.product.application.usecase.ProductQueryUseCase;
import com.product.domain.model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductQueryUseCaseTest {

    @Mock
    private ProductQueryPort queryPort;

    @Mock
    private ProductCachePort cachePort;

    @InjectMocks
    private ProductQueryUseCase queryUseCase;

    @Test
    @DisplayName("U3: search - Cache doluysa doğrudan cache'den döner")
    void search_WhenCacheHit_ShouldReturnCachedData() {
        String cacheKey = "cache:catalog:search:Laptop:Electronics";
        List<Product> cachedProducts = List.of(new Product());

        when(cachePort.getCachedSearchResults(cacheKey)).thenReturn(Optional.of(cachedProducts));

        List<Product> result = queryUseCase.search("Laptop", "Electronics");

        assertThat(result).hasSize(1);
        verify(queryPort, never()).searchProducts(any(), any());
    }

    @Test
    @DisplayName("U4: search - Cache boşsa DB'den çeker ve cache'e yazar")
    void search_WhenCacheMiss_ShouldFetchAndCache() {
        String cacheKey = "cache:catalog:search:Laptop:Electronics";
        List<Product> dbProducts = List.of(new Product());

        when(cachePort.getCachedSearchResults(cacheKey)).thenReturn(Optional.empty());
        when(queryPort.searchProducts("Laptop", "Electronics")).thenReturn(dbProducts);

        List<Product> result = queryUseCase.search("Laptop", "Electronics");

        assertThat(result).hasSize(1);
        verify(queryPort).searchProducts("Laptop", "Electronics");
        verify(cachePort).cacheSearchResults(eq(cacheKey), eq(dbProducts), eq(7200L));
    }
}