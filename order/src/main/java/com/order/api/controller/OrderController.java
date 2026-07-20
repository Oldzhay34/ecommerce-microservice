package com.order.api.controller;

import com.order.api.dto.CreateOrderRequest;
import com.order.api.dto.OrderItemDto;
import com.order.api.dto.OrderResponse;
import com.order.api.dto.UpdateOrderStatusRequest;
import com.order.application.port.in.OrderCommandUseCase;
import com.order.application.port.in.OrderQueryUseCase;
import com.order.domain.exception.UnauthorizedOrderAccessException;
import com.order.domain.model.Order;
import com.order.domain.model.OrderStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderCommandUseCase orderCommandUseCase;
    private final OrderQueryUseCase orderQueryUseCase;

    public OrderController(OrderCommandUseCase orderCommandUseCase, OrderQueryUseCase orderQueryUseCase) {
        this.orderCommandUseCase = orderCommandUseCase;
        this.orderQueryUseCase = orderQueryUseCase;
    }

    @PostMapping
    //@PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
        String authUserId = authentication.getName();
        if (!authUserId.equals(request.getUserId())) {
            throw new UnauthorizedOrderAccessException("IDOR İhlali: Kendi adınıza olmayan sipariş oluşturamazsınız.");
        }

        Order order = orderCommandUseCase.createOrder(request);
        return new ResponseEntity<>(mapToResponse(order), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String id, Authentication authentication) {
        String authUserId = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        Order order = orderQueryUseCase.getOrderById(id, authUserId, role);
        return ResponseEntity.ok(mapToResponse(order));
    }

    @GetMapping("/customer/{userId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getCustomerOrders(@PathVariable String userId, Authentication authentication) {
        String authUserId = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        if (!"ROLE_ADMIN".equals(role) && !authUserId.equals(userId)) {
            throw new UnauthorizedOrderAccessException("IDOR İhlali: Başka kullanıcının siparişlerine erişemezsiniz.");
        }

        List<Order> orders = orderQueryUseCase.getOrdersForCustomer(userId, userId);
        return ResponseEntity.ok(orders.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasRole('STORE') or hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getStoreOrders(@PathVariable String storeId, Authentication authentication) {
        String authUserId = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        if (!"ROLE_ADMIN".equals(role) && !authUserId.equals(storeId)) {
            throw new UnauthorizedOrderAccessException("Sadece kendi mağazanızın siparişlerini görebilirsiniz.");
        }

        List<Order> orders = orderQueryUseCase.getOrdersForStore(storeId);
        return ResponseEntity.ok(orders.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<Order> orders = orderQueryUseCase.getAllOrdersForAdmin();
        return ResponseEntity.ok(orders.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            Authentication authentication) {

        String authUserId = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        Order updatedOrder;
        if (request.getStatus() == OrderStatus.SHIPPED) {
            if (!"ROLE_STORE".equals(role) && !"ROLE_ADMIN".equals(role)) {
                throw new UnauthorizedOrderAccessException("Sadece STORE veya ADMIN siparişi kargolayabilir.");
            }
            updatedOrder = orderCommandUseCase.shipOrder(id, authUserId);
        } else if (request.getStatus() == OrderStatus.CANCELLED) {
            updatedOrder = orderCommandUseCase.cancelOrder(id, authUserId, role);
        } else {
            throw new IllegalArgumentException("Doğrudan bu statüye geçiş izni yok.");
        }

        return ResponseEntity.ok(mapToResponse(updatedOrder));
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemDto(item.getProductId(), item.getStoreId(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                itemDtos
        );
    }
}