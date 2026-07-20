package com.product.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.product.application.port.out.ProductCommandPort;
import com.product.application.usecase.StockReservationUseCase;
import com.product.domain.model.Product;
import com.product.infrastructure.messaging.dto.OrderCreatedEventPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockReservationUseCaseTest {

    @Mock
    private ProductCommandPort commandPort;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private StockReservationUseCase stockReservationUseCase;

    private UUID productId;
    private String orderId;
    private Product product;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID().toString();
        product = new Product(productId, UUID.randomUUID(), "Laptop", "Electronics", BigDecimal.valueOf(1500), 10);
    }

    private OrderCreatedEventPayload buildPayload(int quantity) {
        OrderCreatedEventPayload payload = new OrderCreatedEventPayload();
        payload.setId(orderId);
        payload.setUserId(UUID.randomUUID().toString());

        OrderCreatedEventPayload.OrderItemPayload item = new OrderCreatedEventPayload.OrderItemPayload();
        item.setProductId(productId.toString());
        item.setStoreId(UUID.randomUUID().toString());
        item.setQuantity(quantity);
        item.setPrice(BigDecimal.valueOf(1500));

        payload.setItems(List.of(item));
        return payload;
    }

    @Test
    @DisplayName("U5: reserveStock - Stok yeterliyse düşer ve stock.event.reserved yayınlar")
    void reserveStock_WhenStockSufficient_ShouldDecrementAndPublishReserved() {
        OrderCreatedEventPayload payload = buildPayload(3);

        when(commandPort.findProductById(productId)).thenReturn(Optional.of(product));

        stockReservationUseCase.reserveStock(payload);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(commandPort).saveProduct(productCaptor.capture());
        assertThat(productCaptor.getValue().getStock()).isEqualTo(7); // 10 - 3

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(commandPort).saveOutboxEvent(eq("Order"), eq(orderId), eq("stock.event.reserved"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("StockReservedEvent").contains(orderId);
    }

    @Test
    @DisplayName("U6: reserveStock - Stok yetersizse hiç düşmez ve stock.event.rejected yayınlar")
    void reserveStock_WhenStockInsufficient_ShouldNotDecrementAndPublishRejected() {
        OrderCreatedEventPayload payload = buildPayload(50); // stoktan (10) fazla

        when(commandPort.findProductById(productId)).thenReturn(Optional.of(product));

        stockReservationUseCase.reserveStock(payload);

        verify(commandPort, never()).saveProduct(any());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(commandPort).saveOutboxEvent(eq("Order"), eq(orderId), eq("stock.event.rejected"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("StockRejectedEvent").contains(orderId);
    }

    @Test
    @DisplayName("U7: reserveStock - Ürün bulunamazsa stock.event.rejected yayınlar")
    void reserveStock_WhenProductNotFound_ShouldPublishRejected() {
        OrderCreatedEventPayload payload = buildPayload(1);

        when(commandPort.findProductById(productId)).thenReturn(Optional.empty());

        stockReservationUseCase.reserveStock(payload);

        verify(commandPort, never()).saveProduct(any());
        verify(commandPort).saveOutboxEvent(eq("Order"), eq(orderId), eq("stock.event.rejected"), any());
    }

    @Test
    @DisplayName("U8: reserveStock - Birden fazla kalemde biri yetersizse hiçbiri düşmez (all-or-nothing)")
    void reserveStock_WhenOneOfMultipleItemsInsufficient_ShouldRejectAll() {
        UUID secondProductId = UUID.randomUUID();
        Product secondProduct = new Product(secondProductId, UUID.randomUUID(), "Mouse", "Electronics", BigDecimal.valueOf(50), 2);

        OrderCreatedEventPayload payload = buildPayload(3); // ilk ürün: 10 stok, 3 istek -> yeterli
        OrderCreatedEventPayload.OrderItemPayload secondItem = new OrderCreatedEventPayload.OrderItemPayload();
        secondItem.setProductId(secondProductId.toString());
        secondItem.setStoreId(UUID.randomUUID().toString());
        secondItem.setQuantity(5); // 2 stok, 5 istek -> yetersiz
        secondItem.setPrice(BigDecimal.valueOf(50));
        payload.setItems(List.of(payload.getItems().get(0), secondItem));

        when(commandPort.findProductById(productId)).thenReturn(Optional.of(product));
        when(commandPort.findProductById(secondProductId)).thenReturn(Optional.of(secondProduct));

        stockReservationUseCase.reserveStock(payload);

        verify(commandPort, never()).saveProduct(any());
        verify(commandPort).saveOutboxEvent(eq("Order"), eq(orderId), eq("stock.event.rejected"), any());
    }
}