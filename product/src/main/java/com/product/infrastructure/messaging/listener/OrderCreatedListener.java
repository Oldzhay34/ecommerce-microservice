package com.product.infrastructure.messaging.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.product.application.usecase.StockReservationUseCase;
import com.product.infrastructure.messaging.dto.OrderCreatedEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.product.infrastructure.messaging.config.OrderEventRabbitConfig.STOCK_RESERVATION_QUEUE;

@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final StockReservationUseCase stockReservationUseCase;
    private final ObjectMapper objectMapper;

    public OrderCreatedListener(StockReservationUseCase stockReservationUseCase, ObjectMapper objectMapper) {
        this.stockReservationUseCase = stockReservationUseCase;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = STOCK_RESERVATION_QUEUE)
    public void handleOrderCreated(String message) {
        try {
            OrderCreatedEventPayload payload = objectMapper.readValue(message, OrderCreatedEventPayload.class);
            log.info(">>> OrderCreatedEvent alındı, orderId={}", payload.getId());
            stockReservationUseCase.reserveStock(payload);
        } catch (Exception e) {
            log.error(">>> OrderCreatedEvent işlenirken hata: {}", e.getMessage(), e);
            // RabbitMQ retry/DLQ mekanizmasının devreye girmesi için yeniden fırlatılır.
            throw new RuntimeException("Failed to process OrderCreatedEvent", e);
        }
    }
}