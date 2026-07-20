package com.promptengineering.auth.infrastructure.messaging.publisher;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ECOMMERCE_TOPIC_EXCHANGE = "ecommerce.topic";

    @Bean
    public TopicExchange ecommerceTopicExchange() {
        return new TopicExchange(ECOMMERCE_TOPIC_EXCHANGE, true, false);
    }
}