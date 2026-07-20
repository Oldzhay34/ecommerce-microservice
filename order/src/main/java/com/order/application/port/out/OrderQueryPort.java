package com.order.application.port.out;

import com.order.domain.model.Order;
import java.util.List;
import java.util.Optional;

@org.springframework.modulith.NamedInterface("port.out")
public interface OrderQueryPort {
    Optional<Order> findById(String id);
    List<Order> findByUserId(String userId);
    List<Order> findByStoreId(String storeId);
    List<Order> findAll();
}