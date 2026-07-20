package com.product.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.product.api.dto.CreateProductRequest;
import com.product.api.dto.UpdateStockRequest;
import com.product.application.port.out.ProductCommandPort;
import com.product.application.usecase.ProductCommandUseCase;
import com.product.domain.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCommandUseCaseTest {

    @Mock
    private ProductCommandPort commandPort;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProductCommandUseCase commandUseCase;

    private UUID storeId;
    private Product product;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        product = new Product(UUID.randomUUID(), storeId, "Laptop", "Electronics", BigDecimal.valueOf(1500), 10);
    }

    @Test
    @DisplayName("U1: createProduct - Ürünü kaydeder ve Outbox event fırlatır")
    void createProduct_ShouldSaveAndPublishEvent() throws JsonProcessingException {
        CreateProductRequest request = new CreateProductRequest("Laptop", "Electronics", BigDecimal.valueOf(1500), 10);

        when(commandPort.saveProduct(any(Product.class))).thenReturn(product);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"id\":\"123\"}");

        Product result = commandUseCase.createProduct(request, storeId);

        assertThat(result.getName()).isEqualTo("Laptop");
        verify(commandPort).saveProduct(any(Product.class));
        verify(commandPort).saveOutboxEvent(eq("Product"), eq(product.getId().toString()), eq("catalog.event.created"), anyString());
    }

    @Test
    @DisplayName("U2: updateStock - Yetkisiz mağaza stok güncelleyemez")
    void updateStock_WhenUnauthorizedStore_ShouldThrowException() {
        UpdateStockRequest request = new UpdateStockRequest(20);
        UUID wrongStoreId = UUID.randomUUID();

        when(commandPort.findProductById(product.getId())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> commandUseCase.updateStock(product.getId(), request, wrongStoreId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unauthorized store access");

        verify(commandPort, never()).saveProduct(any());
    }
}