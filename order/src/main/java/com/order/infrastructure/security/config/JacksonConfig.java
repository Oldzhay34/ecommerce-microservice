package com.order.infrastructure.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * Outbox payload'ları bu mapper ile serileştiriliyor. Order/OrderItem
     * modelleri LocalDateTime içerdiği için JavaTimeModule kayıtlı OLMAZSA
     * writeValueAsString() InvalidDefinitionException fırlatır; bu da
     * createOrder'ı komple 500'e düşürür. Tarihleri epoch sayısı yerine
     * ISO-8601 string olarak yazıyoruz.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}