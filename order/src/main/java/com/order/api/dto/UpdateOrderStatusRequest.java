package com.order.api.dto;

import com.order.domain.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

@org.springframework.modulith.NamedInterface("dto")
public class UpdateOrderStatusRequest {

    @NotNull(message = "Sipariş durumu boş olamaz")
    private OrderStatus status;

    public UpdateOrderStatusRequest() {}

    public UpdateOrderStatusRequest(OrderStatus status) {
        this.status = status;
    }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}