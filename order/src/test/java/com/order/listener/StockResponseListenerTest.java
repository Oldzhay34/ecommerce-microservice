package com.order.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.application.port.in.OrderCommandUseCase;
import com.order.infrastructure.messaging.listener.StockResponseListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockResponseListenerTest {

    @Mock
    private OrderCommandUseCase orderCommandUseCase;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private StockResponseListener stockResponseListener;

    @Test
    @DisplayName("U19: handleStockResponse - StockReservedEvent gelirse siparişi onaylar (approveOrder)")
    void handleStockResponse_WithStockReservedEvent_ShouldApproveOrder() {
        String message = "{\"eventType\":\"StockReservedEvent\",\"orderId\":\"order-123\"}";

        stockResponseListener.handleStockResponse(message);

        verify(orderCommandUseCase).approveOrder("order-123");
        verify(orderCommandUseCase, never()).cancelOrder(any(), any(), any());
    }

    @Test
    @DisplayName("U20: handleStockResponse - StockRejectedEvent gelirse siparişi iptal eder (cancelOrder)")
    void handleStockResponse_WithStockRejectedEvent_ShouldCancelOrder() {
        String message = "{\"eventType\":\"StockRejectedEvent\",\"orderId\":\"order-123\"}";

        stockResponseListener.handleStockResponse(message);

        verify(orderCommandUseCase).cancelOrder("order-123", "SYSTEM", "ROLE_ADMIN");
        verify(orderCommandUseCase, never()).approveOrder(any());
    }

    @Test
    @DisplayName("U21: handleStockResponse - Bilinmeyen eventType gelirse hiçbir use case metodu çağrılmaz")
    void handleStockResponse_WithUnknownEventType_ShouldDoNothing() {
        String message = "{\"eventType\":\"SomeOtherEvent\",\"orderId\":\"order-123\"}";

        stockResponseListener.handleStockResponse(message);

        verify(orderCommandUseCase, never()).approveOrder(any());
        verify(orderCommandUseCase, never()).cancelOrder(any(), any(), any());
    }

    @Test
    @DisplayName("U22: handleStockResponse - Bozuk JSON gelirse RuntimeException fırlatır (retry/DLQ tetiklensin)")
    void handleStockResponse_WithMalformedJson_ShouldThrowRuntimeException() {
        String malformedMessage = "{ not valid json";

        assertThatThrownBy(() -> stockResponseListener.handleStockResponse(malformedMessage))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("RabbitMQ mesajı işlenirken hata oluştu");

        verify(orderCommandUseCase, never()).approveOrder(any());
        verify(orderCommandUseCase, never()).cancelOrder(any(), any(), any());
    }
}