package com.mediaservice.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Media servisi YAYINCI olarak media.exchange (topic) kullanir; routing key'ler:
 * media.uploaded / media.deleted / media.updated.
 *
 * Media servisi DINLEYICI olarak product.exchange / product.deleted dinler.
 *
 * Servisler RabbitMQ'ya DOGRUDAN mesaj atmaz: once outbox_event tablosuna yazilir,
 * OutboxEventPublisher @Scheduled ile okuyup basar (Transactional Outbox Pattern).
 */
@Configuration
public class RabbitMqConfig {

    public static final String MEDIA_EXCHANGE = "media.exchange";
    public static final String PRODUCT_EXCHANGE = "product.exchange";

    public static final String RK_MEDIA_UPLOADED = "media.uploaded";
    public static final String RK_MEDIA_DELETED = "media.deleted";
    public static final String RK_MEDIA_UPDATED = "media.updated";

    public static final String RK_PRODUCT_DELETED = "product.deleted";

    public static final String QUEUE_PRODUCT_DELETED = "media.product-deleted.queue";
    public static final String QUEUE_PRODUCT_DELETED_DLQ = "media.product-deleted.dlq";
    public static final String DLX_EXCHANGE = "media.dlx";
    public static final String RK_PRODUCT_DELETED_DLQ = "media.product-deleted.dlq";

    // ------------------------------------------------------------------
    // Yayin tarafi
    // ------------------------------------------------------------------

    @Bean
    public TopicExchange mediaExchange() {
        return new TopicExchange(MEDIA_EXCHANGE, true, false);
    }

    // ------------------------------------------------------------------
    // Dinleme tarafi
    // ------------------------------------------------------------------

    /**
     * [Varsayim 1] Product servisi ProductDeletedEvent yayinliyor mu? Yayinlamiyorsa bu exchange
     * Media servisi tarafindan (durable, ayni tiple) olusturulur ve kuyruk bos kalir - zararsizdir.
     * Product servisi ayni adi ayni tiple bildirdiginde catisma olmaz.
     */
    @Bean
    public TopicExchange productExchange() {
        return new TopicExchange(PRODUCT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue productDeletedQueue() {
        return QueueBuilder.durable(QUEUE_PRODUCT_DELETED)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RK_PRODUCT_DELETED_DLQ)
                .build();
    }

    @Bean
    public Queue productDeletedDlq() {
        return QueueBuilder.durable(QUEUE_PRODUCT_DELETED_DLQ).build();
    }

    @Bean
    public Binding productDeletedBinding(Queue productDeletedQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(productDeletedQueue).to(productExchange).with(RK_PRODUCT_DELETED);
    }

    @Bean
    public Binding productDeletedDlqBinding(Queue productDeletedDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(productDeletedDlq).to(deadLetterExchange).with(RK_PRODUCT_DELETED_DLQ);
    }

    // ------------------------------------------------------------------
    // Serilestirme
    // ------------------------------------------------------------------

    /**
     * Outbox payload'i zaten JSON String olarak saklanir; publisher bunu ham String olarak basar.
     * Converter, listener tarafinda gelen JSON'i okumak icin gereklidir.
     */
    @Bean
    public MessageConverter rabbitMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter rabbitMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(rabbitMessageConverter);
        return template;
    }
}