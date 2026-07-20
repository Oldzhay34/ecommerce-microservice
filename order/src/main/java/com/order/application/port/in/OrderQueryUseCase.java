package com.order.application.port.in;

import com.order.domain.model.Order;
import java.util.List;

@org.springframework.modulith.NamedInterface("port.in")
public interface OrderQueryUseCase {
    Order getOrderById(String orderId, String requestedUserId, String userRole);
    List<Order> getOrdersForCustomer(String userId, String authenticatedUserId);
    List<Order> getOrdersForStore(String storeId);
    List<Order> getAllOrdersForAdmin();
}