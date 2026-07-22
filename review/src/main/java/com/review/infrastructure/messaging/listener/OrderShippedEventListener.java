package com.review.infrastructure.messaging.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.application.port.out.PurchaseEligibilityPort;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderShippedEventListener {

    private final PurchaseEligibilityPort purchaseEligibilityPort;
    private final ObjectMapper objectMapper;

    public OrderShippedEventListener(PurchaseEligibilityPort purchaseEligibilityPort, ObjectMapper objectMapper) {
        this.purchaseEligibilityPort = purchaseEligibilityPort;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "review.order.shipped.queue", durable = "true"),
            exchange = @Exchange(value = "order.exchange", type = "topic", durable = "true"),
            key = "order.shipped"
    ))
    public void handleOrderShippedEvent(String payload) {
        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            String orderId = rootNode.path("orderId").asText();
            String customerId = rootNode.path("customerId").asText();
            JsonNode itemsNode = rootNode.path("items");

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    String productId = item.path("productId").asText();
                    purchaseEligibilityPort.createIdempotent(orderId, customerId, productId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing order.shipped event: " + e.getMessage());
        }
    }
}