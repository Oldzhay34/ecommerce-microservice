package com.product.infrastructure.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CATALOG_SYNC_QUEUE = "search.catalog.sync.q";
    public static final String ECOMMERCE_EXCHANGE = "ecommerce.topic";
    public static final String CATALOG_EVENT_ROUTING_PATTERN = "catalog.event.*";

    @Bean
    public Queue catalogSyncQueue() {
        return QueueBuilder.durable(CATALOG_SYNC_QUEUE).build();
    }

    @Bean
    public TopicExchange ecommerceExchange() {
        return new TopicExchange(ECOMMERCE_EXCHANGE);
    }

    @Bean
    public Binding catalogSyncBinding(Queue catalogSyncQueue, TopicExchange ecommerceExchange) {
        return BindingBuilder.bind(catalogSyncQueue)
                .to(ecommerceExchange)
                .with(CATALOG_EVENT_ROUTING_PATTERN);
    }
}