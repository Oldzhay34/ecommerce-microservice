package com.notificationservice.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.topic}")
    private String topicExchangeName;

    @Value("${rabbitmq.queue.otp}")
    private String otpQueueName;

    @Value("${rabbitmq.queue.otp-dlq}")
    private String otpDlqName;

    @Value("${rabbitmq.routing-key.otp}")
    private String otpRoutingKey;

    @Bean
    public TopicExchange ecommerceTopicExchange() {
        return new TopicExchange(topicExchangeName);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("notification.email.otp.dlx");
    }

    @Bean
    public Queue otpQueue() {
        return QueueBuilder.durable(otpQueueName)
                .withArgument("x-dead-letter-exchange", "notification.email.otp.dlx")
                .withArgument("x-dead-letter-routing-key", otpDlqName)
                .build();
    }

    @Bean
    public Queue otpDlq() {
        return QueueBuilder.durable(otpDlqName).build();
    }

    @Bean
    public Binding otpQueueBinding(Queue otpQueue, TopicExchange ecommerceTopicExchange) {
        return BindingBuilder.bind(otpQueue).to(ecommerceTopicExchange).with(otpRoutingKey);
    }

    @Bean
    public Binding otpDlqBinding(Queue otpDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(otpDlq).to(dlxExchange).with(otpDlqName);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // IDE uyarılarını ezer, Spring Boot runtime'da bu bean'i bulacaktır.
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAdviceChain(RetryInterceptorBuilder
                .stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 10000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }
}