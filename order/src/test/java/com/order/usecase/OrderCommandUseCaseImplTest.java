package com.order.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.api.dto.CreateOrderRequest;
import com.order.api.dto.OrderItemDto;
import com.order.application.port.out.OrderCommandPort;
import com.order.application.port.out.OrderQueryPort;
import com.order.application.usecase.OrderCommandUseCaseImpl;
import com.order.domain.exception.OrderNotFoundException;
import com.order.domain.exception.UnauthorizedOrderAccessException;
import com.order.domain.model.Order;
import com.order.domain.model.OrderItem;
import com.order.domain.model.OrderStatus;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCommandUseCaseImplTest {

    @Mock
    private OrderCommandPort orderCommandPort;

    @Mock
    private OrderQueryPort orderQueryPort;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private OrderCommandUseCaseImpl orderCommandUseCase;

    private String orderId;
    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID().toString();

        OrderItem item = new OrderItem(UUID.randomUUID().toString(), "prod-1", "store-1", 2, BigDecimal.valueOf(50));
        pendingOrder = new Order(orderId, "user-1", List.of(item), OrderStatus.PENDING, BigDecimal.valueOf(100), LocalDateTime.now());
    }

    @Test
    @DisplayName("U1: createOrder - Siparişi PENDING olarak kaydeder, toplam tutarı hesaplar ve outbox event yaratır")
    void createOrder_ShouldSaveAsPendingAndPublishOutboxEvent() {
        CreateOrderRequest request = new CreateOrderRequest("user-1",
                List.of(new OrderItemDto("prod-1", "store-1", 2, BigDecimal.valueOf(50))));

        when(orderCommandPort.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderCommandUseCase.createOrder(request);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.getUserId()).isEqualTo("user-1");

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(orderCommandPort).saveOutboxEvent(eq(result.getId()), eq("Order"), eq("OrderCreatedEvent"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("user-1").contains("prod-1");
    }

    @Test
    @DisplayName("U2: approveOrder - Siparişi APPROVED yapar ve OrderApprovedEvent yaratır")
    void approveOrder_WhenOrderExists_ShouldSetApprovedAndPublishEvent() {
        when(orderQueryPort.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(orderCommandPort.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderCommandUseCase.approveOrder(orderId);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.APPROVED);
        verify(orderCommandPort).saveOutboxEvent(eq(orderId), eq("Order"), eq("OrderApprovedEvent"), any());
    }

    @Test
    @DisplayName("U3: approveOrder - Sipariş bulunamazsa OrderNotFoundException fırlatır")
    void approveOrder_WhenOrderNotFound_ShouldThrowException() {
        when(orderQueryPort.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderCommandUseCase.approveOrder(orderId))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("U4: cancelOrder - Sipariş sahibi kendi siparişini iptal edebilir")
    void cancelOrder_WhenRequestedByOwner_ShouldCancelAndPublishEvent() {
        when(orderQueryPort.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(orderCommandPort.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderCommandUseCase.cancelOrder(orderId, "user-1", "ROLE_CUSTOMER");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderCommandPort).saveOutboxEvent(eq(orderId), eq("Order"), eq("OrderCancelledEvent"), any());
    }

    @Test
    @DisplayName("U5: cancelOrder - Başkasının siparişini iptal etmeye çalışırsa UnauthorizedOrderAccessException fırlatır")
    void cancelOrder_WhenRequestedByNonOwnerNonAdmin_ShouldThrowUnauthorized() {
        when(orderQueryPort.findById(orderId)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderCommandUseCase.cancelOrder(orderId, "another-user", "ROLE_CUSTOMER"))
                .isInstanceOf(UnauthorizedOrderAccessException.class);

        verify(orderCommandPort, never()).save(any());
    }

    @Test
    @DisplayName("U6: cancelOrder - ADMIN rolü başkasının siparişini de iptal edebilir")
    void cancelOrder_WhenRequestedByAdmin_ShouldCancelRegardlessOfOwnership() {
        when(orderQueryPort.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(orderCommandPort.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderCommandUseCase.cancelOrder(orderId, "admin-user", "ROLE_ADMIN");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("U7: shipOrder - Siparişte mağazaya ait ürün varsa SHIPPED yapar")
    void shipOrder_WhenOrderContainsStoreProduct_ShouldShipAndPublishEvent() {
        when(orderQueryPort.findById(orderId)).thenReturn(Optional.of(pendingOrder));
        when(orderCommandPort.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderCommandUseCase.shipOrder(orderId, "store-1");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(orderCommandPort).saveOutboxEvent(eq(orderId), eq("Order"), eq("OrderShippedEvent"), any());
    }

    @Test
    @DisplayName("U8: shipOrder - Siparişte mağazaya ait ürün yoksa UnauthorizedOrderAccessException fırlatır")
    void shipOrder_WhenOrderDoesNotContainStoreProduct_ShouldThrowUnauthorized() {
        when(orderQueryPort.findById(orderId)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderCommandUseCase.shipOrder(orderId, "unrelated-store"))
                .isInstanceOf(UnauthorizedOrderAccessException.class);

        verify(orderCommandPort, never()).save(any());
    }
}