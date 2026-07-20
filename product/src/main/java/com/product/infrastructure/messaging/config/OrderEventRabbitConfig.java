package com.product.infrastructure.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Order servisinin ürettiği "order.created" event'ini dinlemek için gereken
 * exchange/queue/binding tanımı.
 *
 * KONTRAT (order-service ile paylaşılan):
 *   exchange     : order.exchange (order-service: OutboxEventPublisher.EXCHANGE)
 *   routing key  : order.created  (order-service: getRoutingKey("OrderCreatedEvent"))
 *   queue (bizim): product.stock.reservation.q
 *
 * NOT: order.exchange'i burada da bean olarak deklare ediyoruz çünkü her iki
 * servis de kendi RabbitAdmin'i üzerinden bu exchange'in var olduğunu garanti
 * etmeli (topology bağımsız kurulmalı, servis başlatma sırasına bağlı olmamalı).
 */
@Configuration
public class OrderEventRabbitConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String STOCK_RESERVATION_QUEUE = "product.stock.reservation.q";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue stockReservationQueue() {
        return QueueBuilder.durable(STOCK_RESERVATION_QUEUE).build();
    }

    @Bean
    public Binding stockReservationBinding(Queue stockReservationQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(stockReservationQueue)
                .to(orderExchange)
                .with(ORDER_CREATED_ROUTING_KEY);
    }
}