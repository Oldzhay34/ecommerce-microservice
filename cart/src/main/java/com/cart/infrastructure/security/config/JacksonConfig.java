package com.cart.infrastructure.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * BUG FIX: Burada daha önce çıplak {@code new ObjectMapper()} dönülüyordu.
     * Bu bean hem outbox payload'larını (CartCommandUseCaseImpl#publishEvent)
     * hem de gelen OrderCreatedEvent payload'larını
     * (OrderCreatedEventListener) işliyor. JavaTimeModule kayıtlı DEĞİLSE
     * herhangi bir java.time alanı {@code writeValueAsString()} sırasında
     * InvalidDefinitionException fırlatır ve outbox yazımını -dolayısıyla tüm
     * sepet komutunu- 500'e düşürür. Tarihler epoch sayısı yerine ISO-8601
     * string olarak yazılır.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
