package com.order.usecase;

import com.order.application.port.out.OrderQueryPort;
import com.order.application.usecase.OrderQueryUseCaseImpl;
import com.order.domain.exception.OrderNotFoundException;
import com.order.domain.exception.UnauthorizedOrderAccessException;
import com.order.domain.model.Order;
import com.order.domain.model.OrderItem;
import com.order.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderQueryUseCaseImplTest {

    @Mock
    private OrderQueryPort orderQueryPort;

    @InjectMocks
    private OrderQueryUseCaseImpl orderQueryUseCase;

    private String orderId;
    private Order order;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID().toString();
        OrderItem item = new OrderItem(UUID.randomUUID().toString(), "prod-1", "store-1", 2, BigDecimal.valueOf(50));
        order = new Order(orderId, "user-1", List.of(item), OrderStatus.PENDING, BigDecimal.valueOf(100), LocalDateTime.now());
    }

    @Test
    @DisplayName("U9: getOrderById - ADMIN her siparişi görebilir")
    void getOrderById_WhenRequestedByAdmin_ShouldReturnOrder() {
        when(orderQueryPort.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderQueryUseCase.getOrderById(orderId, "admin-user", "ROLE_ADMIN");

        assertThat(result.getId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("U10: getOrderById - Sipariş sahibi CUSTOMER kendi siparişini görebilir")
    void getOrderById_WhenRequestedByOwner_ShouldReturnOrder() {
        when(orderQueryPort.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderQueryUseCase.getOrderById(orderId, "user-1", "ROLE_CUSTOMER");

        assertThat(result.getId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("U11: getOrderById - CUSTOMER başkasının siparişini göremez (IDOR koruması)")
    void getOrderById_WhenRequestedByNonOwnerCustomer_ShouldThrowUnauthorized() {
        when(orderQueryPort.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderQueryUseCase.getOrderById(orderId, "another-user", "ROLE_CUSTOMER"))
                .isInstanceOf(UnauthorizedOrderAccessException.class);
    }

    @Test
    @DisplayName("U12: getOrderById - STORE, siparişte kendi ürünü varsa görebilir")
    void getOrderById_WhenRequestedByOwningStore_ShouldReturnOrder() {
        when(orderQueryPort.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderQueryUseCase.getOrderById(orderId, "store-1", "ROLE_STORE");

        assertThat(result.getId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("U13: getOrderById - STORE, siparişte kendi ürünü yoksa göremez")
    void getOrderById_WhenRequestedByNonOwningStore_ShouldThrowUnauthorized() {
        when(orderQueryPort.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderQueryUseCase.getOrderById(orderId, "unrelated-store", "ROLE_STORE"))
                .isInstanceOf(UnauthorizedOrderAccessException.class);
    }

    @Test
    @DisplayName("U14: getOrderById - Sipariş bulunamazsa OrderNotFoundException fırlatır")
    void getOrderById_WhenOrderNotFound_ShouldThrowNotFound() {
        when(orderQueryPort.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderQueryUseCase.getOrderById(orderId, "user-1", "ROLE_CUSTOMER"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("U15: getOrdersForCustomer - Kendi userId'si ile sorgulayan kullanıcı listeyi alır")
    void getOrdersForCustomer_WhenMatchingUser_ShouldReturnOrders() {
        when(orderQueryPort.findByUserId("user-1")).thenReturn(List.of(order));

        List<Order> result = orderQueryUseCase.getOrdersForCustomer("user-1", "user-1");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("U16: getOrdersForCustomer - Başka kullanıcının siparişleri istenirse IDOR hatası fırlatır")
    void getOrdersForCustomer_WhenMismatchedUser_ShouldThrowUnauthorized() {
        assertThatThrownBy(() -> orderQueryUseCase.getOrdersForCustomer("user-1", "another-user"))
                .isInstanceOf(UnauthorizedOrderAccessException.class);
    }

    @Test
    @DisplayName("U17: getOrdersForStore - Mağaza kendi siparişlerini alabilir")
    void getOrdersForStore_ShouldReturnOrdersForStore() {
        when(orderQueryPort.findByStoreId("store-1")).thenReturn(List.of(order));

        List<Order> result = orderQueryUseCase.getOrdersForStore("store-1");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("U18: getAllOrdersForAdmin - Tüm siparişleri döner")
    void getAllOrdersForAdmin_ShouldReturnAllOrders() {
        when(orderQueryPort.findAll()).thenReturn(List.of(order));

        List<Order> result = orderQueryUseCase.getAllOrdersForAdmin();

        assertThat(result).hasSize(1);
    }
}