package com.gateway.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Unit testlerde Spring Boot'un uygulama baglaminda urettigi ObjectMapper'in
 * davranisini taklit eder (JavaTimeModule kayitli, tarihler ISO-8601 string).
 * Duz {@code new ObjectMapper()} LocalDateTime'i serialize edemez ve test
 * yanlislikla production kodundaki fallback dalini olcerdi.
 */
public final class TestObjectMappers {

    private TestObjectMappers() {
    }

    public static ObjectMapper springBootLike() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
