package com.payment.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_APPROVED_QUEUE = "order.approved.queue";
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_APPROVED_ROUTING_KEY = "order.approved";

    /**
     * BUG DÜZELTMESİ: OutboxEventPublisher olayları "payment.exchange"e yayınlıyordu
     * ama bu exchange HİÇBİR YERDE TANIMLI DEĞİLDİ. Var olmayan bir exchange'e
     * yayınlanan mesajı broker sessizce düşürür (publisher confirm açık olmadığı için
     * istemci hata da almaz); outbox satırı ise yayınlandı sayılıp SİLİNİYORDU.
     * Sonuç: her ödeme/iade olayı kalıcı olarak kayboluyordu.
     */
    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue orderApprovedQueue() {
        return QueueBuilder.durable(ORDER_APPROVED_QUEUE).build();
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Binding orderApprovedBinding(Queue orderApprovedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderApprovedQueue)
                .to(orderExchange)
                .with(ORDER_APPROVED_ROUTING_KEY);
    }
}