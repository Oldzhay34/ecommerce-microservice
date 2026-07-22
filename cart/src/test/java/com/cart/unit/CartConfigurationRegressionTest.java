package com.cart.unit;

import com.cart.CartApplication;
import com.cart.api.dto.AddToCartRequest;
import com.cart.infrastructure.messaging.publisher.OutboxEventPublisher;
import com.cart.infrastructure.persistence.repository.CartRepository;
import com.cart.infrastructure.security.config.JacksonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Katman: UNIT - regresyon kilitleri.
 * Bu sınıftaki her test, cart servisinde tespit edilip düzeltilen GERÇEK bir
 * production bug'ının geri gelmesini engeller. Testler zayıflatılmamalıdır.
 */
@DisplayName("UNIT - Cart konfigürasyon regresyonları (bulunan production bug'ları)")
class CartConfigurationRegressionTest {

    /**
     * BUG 1: JacksonConfig çıplak new ObjectMapper() dönüyordu. Outbox
     * payload'ları bu mapper ile yazıldığı için herhangi bir java.time alanı
     * InvalidDefinitionException'a ve komutun tamamen 500'e düşmesine yol
     * açıyordu.
     */
    @Test
    @DisplayName("U62: JacksonConfig - ObjectMapper java.time tiplerini serileştirebilmelidir (JavaTimeModule kayıtlı)")
    void objectMapper_ShouldSerializeJavaTimeTypesWithoutException() throws Exception {
        ObjectMapper mapper = new JacksonConfig().objectMapper();

        assertThatCode(() -> mapper.writeValueAsString(new HasTimestamp(LocalDateTime.of(2026, 1, 2, 3, 4, 5))))
                .doesNotThrowAnyException();

        String json = mapper.writeValueAsString(new HasTimestamp(LocalDateTime.of(2026, 1, 2, 3, 4, 5)));
        assertThat(json).contains("2026-01-02T03:04:05");
    }

    /**
     * BUG 2: CartApplication'da @EnableScheduling yoktu. OutboxEventPublisher
     * @Scheduled ile işaretli olmasına rağmen scheduling altyapısı hiç
     * kurulmadığı için ASLA çalışmıyor, outbox satırları processed=false olarak
     * birikiyor ve RabbitMQ'ya tek bir sepet olayı bile yayınlanmıyordu.
     */
    @Test
    @DisplayName("U63: CartApplication - @EnableScheduling bulunmalı, aksi halde outbox publisher hiç çalışmaz")
    void cartApplication_ShouldEnableScheduling() {
        assertThat(CartApplication.class.getAnnotation(EnableScheduling.class))
                .as("@EnableScheduling yoksa OutboxEventPublisher#publishEvents hiç tetiklenmez")
                .isNotNull();
    }

    @Test
    @DisplayName("U64: OutboxEventPublisher - Yayın periyodu property ile ayarlanabilir olmalı (testlerde deterministik kapatma)")
    void outboxEventPublisher_ShouldExposeConfigurablePublishRate() throws Exception {
        Method method = OutboxEventPublisher.class.getMethod("publishEvents");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.fixedDelayString()).contains("app.outbox.publish-rate-ms");
    }

    /**
     * BUG 3: AddToCartRequest.quantity sadece @Min(1) taşıyordu. Bean
     * Validation'da @Min null'ı GEÇERLİ sayar; alan hiç gönderilmediğinde
     * istek doğrulamadan geçip use case içinde NullPointerException'a (500)
     * dönüşüyordu.
     */
    @Test
    @DisplayName("U65: AddToCartRequest - quantity null gönderilirse doğrulama hatası üretmelidir")
    void addToCartRequest_WhenQuantityIsNull_ShouldFailValidation() {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(UUID.randomUUID());
        request.setPrice(new BigDecimal("10.00"));
        request.setQuantity(null);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            assertThat(validator.validate(request))
                    .as("quantity null iken istek 400 ile reddedilmeli, use case'de NPE'ye dönüşmemeli")
                    .isNotEmpty();
        }
    }

    /**
     * BUG 4: CartRepository.findByUserId üzerinde @EntityGraph yoktu.
     * CartJpaEntity.items LAZY olduğu için mapper koleksiyona dokunduğunda
     * açık persistence context dışında LazyInitializationException, içindeyken
     * de sepet başına ek SELECT (N+1) oluşuyordu.
     */
    @Test
    @DisplayName("U66: CartRepository - findByUserId @EntityGraph ile items'ı eager getirmelidir")
    void cartRepository_FindByUserId_ShouldFetchItemsViaEntityGraph() throws Exception {
        Method method = CartRepository.class.getMethod("findByUserId", UUID.class);
        EntityGraph entityGraph = method.getAnnotation(EntityGraph.class);

        assertThat(entityGraph)
                .as("@EntityGraph yoksa mapper koleksiyona dokunduğunda LazyInitializationException riski var")
                .isNotNull();
        assertThat(entityGraph.attributePaths()).contains("items");
    }

    static class HasTimestamp {
        private final LocalDateTime createdAt;

        HasTimestamp(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
}
