package com.order.application.usecase;

import com.order.application.port.in.OrderQueryUseCase;
import com.order.application.port.out.OrderQueryPort;
import com.order.domain.exception.OrderNotFoundException;
import com.order.domain.exception.UnauthorizedOrderAccessException;
import com.order.domain.model.Order;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@org.springframework.modulith.NamedInterface("usecase")
public class OrderQueryUseCaseImpl implements OrderQueryUseCase {

    private final OrderQueryPort orderQueryPort;

    public OrderQueryUseCaseImpl(OrderQueryPort orderQueryPort) {
        this.orderQueryPort = orderQueryPort;
    }

    @Override
    public Order getOrderById(String orderId, String requestedUserId, String userRole) {
        Order order = orderQueryPort.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Sipariş bulunamadı: " + orderId));

        if ("ROLE_ADMIN".equals(userRole)) {
            return order;
        }

        if ("ROLE_CUSTOMER".equals(userRole) && !order.getUserId().equals(requestedUserId)) {
            throw new UnauthorizedOrderAccessException("Bu siparişi görüntüleme yetkiniz yok.");
        }

        if ("ROLE_STORE".equals(userRole)) {
            boolean belongsToStore = order.getItems().stream()
                    .anyMatch(item -> item.getStoreId().equals(requestedUserId));
            if (!belongsToStore) {
                throw new UnauthorizedOrderAccessException("Sipariş mağazanıza ait değil.");
            }
        }

        return order;
    }

    @Override
    public List<Order> getOrdersForCustomer(String userId, String authenticatedUserId) {
        if (!userId.equals(authenticatedUserId)) {
            throw new UnauthorizedOrderAccessException("IDOR İhlali: Başka kullanıcının siparişlerine erişemezsiniz.");
        }
        return orderQueryPort.findByUserId(userId);
    }

    @Override
    public List<Order> getOrdersForStore(String storeId) {
        return orderQueryPort.findByStoreId(storeId);
    }

    @Override
    public List<Order> getAllOrdersForAdmin() {
        return orderQueryPort.findAll();
    }
}