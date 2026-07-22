package com.cart.infrastructure.messaging.listener;

import com.cart.application.port.in.CartCommandUseCase;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
@org.springframework.modulith.NamedInterface("infrastructure.messaging.listener")
public class OrderCreatedEventListener {

    private final CartCommandUseCase cartCommandUseCase;
    private final ObjectMapper objectMapper;

    public OrderCreatedEventListener(CartCommandUseCase cartCommandUseCase, ObjectMapper objectMapper) {
        this.cartCommandUseCase = cartCommandUseCase;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "cart.order.created.queue", durable = "true"),
            exchange = @Exchange(value = "order.exchange", type = "topic"),
            key = "order.created"
    ))
    public void handleOrderCreatedEvent(String payload) {
        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            // Payload'dan gelen userId'yi UUID olarak okuyoruz
            String userIdStr = jsonNode.get("userId").asText();
            UUID userId = UUID.fromString(userIdStr);

            cartCommandUseCase.clearCart(userId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process OrderCreatedEvent", e);
        }
    }
}