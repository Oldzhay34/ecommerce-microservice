package com.order.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// TO DO : Product tarafına listener yazılacak
@Configuration
public class StockResponseRabbitConfig {

    public static final String STOCK_RESPONSE_QUEUE = "order.stock.response.queue";
    public static final String STOCK_EXCHANGE = "ecommerce.topic"; // product-service'inkiyle tutarlı
    public static final String STOCK_RESPONSE_ROUTING_KEY = "stock.event.*"; // henüz gerçek publisher yok

    @Bean
    public Queue stockResponseQueue() {
        return QueueBuilder.durable(STOCK_RESPONSE_QUEUE).build();
    }

    @Bean
    public TopicExchange stockExchange() {
        return new TopicExchange(STOCK_EXCHANGE);
    }

    @Bean
    public Binding stockResponseBinding(Queue stockResponseQueue, TopicExchange stockExchange) {
        return BindingBuilder.bind(stockResponseQueue)
                .to(stockExchange)
                .with(STOCK_RESPONSE_ROUTING_KEY);
    }
}