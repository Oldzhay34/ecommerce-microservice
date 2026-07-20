package com.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@org.springframework.modulith.NamedInterface("dto")
public class CreateOrderRequest {

    @NotBlank(message = "User ID boş olamaz")
    private String userId;

    @NotEmpty(message = "Sipariş en az bir ürün içermelidir")
    @Valid
    private List<OrderItemDto> items;

    public CreateOrderRequest() {}

    public CreateOrderRequest(String userId, List<OrderItemDto> items) {
        this.userId = userId;
        this.items = items;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }
}