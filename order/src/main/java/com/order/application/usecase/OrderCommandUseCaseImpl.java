package com.order.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.api.dto.CreateOrderRequest;
import com.order.application.port.in.OrderCommandUseCase;
import com.order.application.port.out.OrderCommandPort;
import com.order.application.port.out.OrderQueryPort;
import com.order.domain.exception.OrderNotFoundException;
import com.order.domain.exception.UnauthorizedOrderAccessException;
import com.order.domain.model.Order;
import com.order.domain.model.OrderItem;
import com.order.domain.model.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@org.springframework.modulith.NamedInterface("usecase")
public class OrderCommandUseCaseImpl implements OrderCommandUseCase {

    private final OrderCommandPort orderCommandPort;
    private final OrderQueryPort orderQueryPort;
    private final ObjectMapper objectMapper;

    public OrderCommandUseCaseImpl(OrderCommandPort orderCommandPort, OrderQueryPort orderQueryPort, ObjectMapper objectMapper) {
        this.orderCommandPort = orderCommandPort;
        this.orderQueryPort = orderQueryPort;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setUserId(request.getUserId());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        List<OrderItem> items = request.getItems().stream().map(dto -> {
            OrderItem item = new OrderItem();
            item.setId(UUID.randomUUID().toString());
            item.setProductId(dto.getProductId());
            item.setStoreId(dto.getStoreId());
            item.setQuantity(dto.getQuantity());
            item.setPrice(dto.getPrice());
            return item;
        }).collect(Collectors.toList());

        order.setItems(items);
        order.calculateTotalAmount();

        Order savedOrder = orderCommandPort.save(order);

        // Transactional Outbox Pattern: Save event to postgres database outbox_event table
        createOutboxEvent(savedOrder.getId(), "Order", "OrderCreatedEvent", savedOrder);

        return savedOrder;
    }

    @Override
    @Transactional
    public Order approveOrder(String orderId) {
        Order order = orderQueryPort.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Sipariş bulunamadı: " + orderId));

        order.setStatus(OrderStatus.APPROVED);
        Order updatedOrder = orderCommandPort.save(order);

        createOutboxEvent(updatedOrder.getId(), "Order", "OrderApprovedEvent", updatedOrder);
        return updatedOrder;
    }

    @Override
    @Transactional
    public Order cancelOrder(String orderId, String requestedUserId, String userRole) {
        Order order = orderQueryPort.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Sipariş bulunamadı: " + orderId));

        if (!"ROLE_ADMIN".equals(userRole) && !order.getUserId().equals(requestedUserId)) {
            throw new UnauthorizedOrderAccessException("Bu siparişi iptal etme yetkiniz yok.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderCommandPort.save(order);

        createOutboxEvent(updatedOrder.getId(), "Order", "OrderCancelledEvent", updatedOrder);
        return updatedOrder;
    }

    @Override
    @Transactional
    public Order shipOrder(String orderId, String requestedStoreId) {
        Order order = orderQueryPort.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Sipariş bulunamadı: " + orderId));

        boolean containsStoreProduct = order.getItems().stream()
                .anyMatch(item -> item.getStoreId().equals(requestedStoreId));

        if (!containsStoreProduct) {
            throw new UnauthorizedOrderAccessException("Sipariş mağazanıza ait ürün içermemektedir.");
        }

        order.setStatus(OrderStatus.SHIPPED);
        Order updatedOrder = orderCommandPort.save(order);

        createOutboxEvent(updatedOrder.getId(), "Order", "OrderShippedEvent", updatedOrder);
        return updatedOrder;
    }

    private void createOutboxEvent(String aggregateId, String aggregateType, String eventType, Object payloadObject) {
        try {
            String payload = objectMapper.writeValueAsString(payloadObject);
            orderCommandPort.saveOutboxEvent(aggregateId, aggregateType, eventType, payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Outbox JSON serileştirme hatası", e);
        }
    }
}