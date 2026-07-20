package com.order.infrastructure.messaging.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.application.port.in.OrderCommandUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class StockResponseListener {

    private final OrderCommandUseCase orderCommandUseCase;
    private final ObjectMapper objectMapper;

    public StockResponseListener(OrderCommandUseCase orderCommandUseCase, ObjectMapper objectMapper) {
        this.orderCommandUseCase = orderCommandUseCase;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "order.stock.response.queue")
    public void handleStockResponse(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String eventType = jsonNode.get("eventType").asText();
            String orderId = jsonNode.get("orderId").asText();

            if ("StockReservedEvent".equals(eventType)) {
                orderCommandUseCase.approveOrder(orderId);
            } else if ("StockRejectedEvent".equals(eventType)) {
                orderCommandUseCase.cancelOrder(orderId, "SYSTEM", "ROLE_ADMIN");
            }
        } catch (Exception e) {
            throw new RuntimeException("RabbitMQ mesajı işlenirken hata oluştu", e);
        }
    }
}