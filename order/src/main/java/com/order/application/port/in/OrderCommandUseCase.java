package com.order.application.port.in;

import com.order.api.dto.CreateOrderRequest;
import com.order.domain.model.Order;

@org.springframework.modulith.NamedInterface("port.in")
public interface OrderCommandUseCase {
    Order createOrder(CreateOrderRequest request);
    Order approveOrder(String orderId);
    Order cancelOrder(String orderId, String requestedUserId, String userRole);
    Order shipOrder(String orderId, String requestedStoreId);
}