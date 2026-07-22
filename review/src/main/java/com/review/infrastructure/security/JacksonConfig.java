package com.review.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * BUGFIX: Burada daha önce çıplak {@code new ObjectMapper()} dönülüyordu.
 * Çıplak bir Jackson 2 ObjectMapper java.time tiplerini (LocalDateTime vb.)
 * TANIMAZ; LocalDateTime içeren herhangi bir nesne (Review, ReviewResponse,
 * outbox payload'ı ...) bu mapper ile serialize edilmeye çalışıldığında
 * {@code InvalidDefinitionException: Java 8 date/time type
 * `java.time.LocalDateTime` not supported by default} fırlatılır ve outbox
 * yazımı patlar (order-service'te birebir aynı hata görüldü).
 *
 * Çözüm: JavaTimeModule kaydedilir ve tarihler epoch timestamp yerine ISO-8601
 * string olarak yazılır, böylece tüketici servislerle sözleşme okunabilir kalır.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
