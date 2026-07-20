package com.product.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.product.application.usecase.StockReservationUseCase;
import com.product.infrastructure.messaging.dto.OrderCreatedEventPayload;
import com.product.infrastructure.messaging.listener.OrderCreatedListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreatedListenerTest {

    @Mock
    private StockReservationUseCase stockReservationUseCase;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private OrderCreatedListener orderCreatedListener;

    @Test
    @DisplayName("U9: handleOrderCreated - Geçerli mesajı parse edip use case'i çağırır")
    void handleOrderCreated_WithValidMessage_ShouldInvokeUseCase() {
        String message = """
                {
                  "id": "order-123",
                  "userId": "user-1",
                  "status": "PENDING",
                  "totalAmount": 100.00,
                  "createdAt": "2026-07-20T10:00:00",
                  "items": [
                    {"id":"item-1","productId":"prod-1","storeId":"store-1","quantity":2,"price":50.00}
                  ]
                }
                """;

        orderCreatedListener.handleOrderCreated(message);

        ArgumentCaptor<OrderCreatedEventPayload> captor = ArgumentCaptor.forClass(OrderCreatedEventPayload.class);
        verify(stockReservationUseCase).reserveStock(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("order-123");
        assertThat(captor.getValue().getItems()).hasSize(1);
    }

    @Test
    @DisplayName("U10: handleOrderCreated - Bozuk JSON gelirse exception fırlatır (retry/DLQ tetiklensin diye)")
    void handleOrderCreated_WithMalformedJson_ShouldThrowException() {
        String malformedMessage = "{ this is not valid json ";

        assertThatThrownBy(() -> orderCreatedListener.handleOrderCreated(malformedMessage))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process OrderCreatedEvent");

        verify(stockReservationUseCase, never()).reserveStock(any());
    }

    @Test
    @DisplayName("U11: handleOrderCreated - Use case exception fırlatırsa listener da fırlatır")
    void handleOrderCreated_WhenUseCaseThrows_ShouldPropagateException() {
        String message = """
                {"id":"order-123","userId":"user-1","items":[{"id":"item-1","productId":"prod-1","storeId":"store-1","quantity":2,"price":50.00}]}
                """;

        doThrow(new RuntimeException("DB connection lost")).when(stockReservationUseCase).reserveStock(any());

        assertThatThrownBy(() -> orderCreatedListener.handleOrderCreated(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process OrderCreatedEvent");
    }
}