package com.review.unit.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.application.port.out.PurchaseEligibilityPort;
import com.review.infrastructure.messaging.listener.OrderShippedEventListener;
import com.review.infrastructure.security.JacksonConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * order.shipped event'inin satın alma uygunluğuna (PurchaseEligibility)
 * dönüşümünün whitebox testi. ObjectMapper, uygulamanın gerçekten kullandığı
 * {@link JacksonConfig} bean'i ile kurulur - böylece Jackson yapılandırmasının
 * regresyonu bu testte de yakalanır.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Review Service - Unit: OrderShippedEventListener")
class OrderShippedEventListenerTest {

    @Mock
    private PurchaseEligibilityPort eligibilityPort;

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    private OrderShippedEventListener listener() {
        return new OrderShippedEventListener(eligibilityPort, objectMapper);
    }

    @Test
    @DisplayName("U41: handleOrderShippedEvent - Siparişteki her ürün için idempotent uygunluk kaydı yaratır")
    void handleOrderShippedEvent_WithMultipleItems_ShouldCreateEligibilityPerProduct() {
        String payload = """
                {"orderId":"order-1","customerId":"cust-1",
                 "items":[{"productId":"prod-1"},{"productId":"prod-2"}]}
                """;

        listener().handleOrderShippedEvent(payload);

        verify(eligibilityPort).createIdempotent("order-1", "cust-1", "prod-1");
        verify(eligibilityPort).createIdempotent("order-1", "cust-1", "prod-2");
    }

    @Test
    @DisplayName("U42: handleOrderShippedEvent - LocalDateTime içeren event payload'ı sorunsuz okunur")
    void handleOrderShippedEvent_WithTimestampField_ShouldStillCreateEligibility() {
        String payload = """
                {"orderId":"order-9","customerId":"cust-9","shippedAt":"2026-01-15T10:30:00",
                 "items":[{"productId":"prod-9","quantity":2}]}
                """;

        listener().handleOrderShippedEvent(payload);

        verify(eligibilityPort).createIdempotent("order-9", "cust-9", "prod-9");
    }

    @Test
    @DisplayName("U43: handleOrderShippedEvent - items alanı yoksa hiçbir uygunluk kaydı yaratılmaz")
    void handleOrderShippedEvent_WhenItemsMissing_ShouldCreateNothing() {
        listener().handleOrderShippedEvent("{\"orderId\":\"order-1\",\"customerId\":\"cust-1\"}");

        verifyNoInteractions(eligibilityPort);
    }

    @Test
    @DisplayName("U44: handleOrderShippedEvent - items dizi değilse (obje) hiçbir kayıt yaratılmaz")
    void handleOrderShippedEvent_WhenItemsIsNotAnArray_ShouldCreateNothing() {
        listener().handleOrderShippedEvent(
                "{\"orderId\":\"order-1\",\"customerId\":\"cust-1\",\"items\":{\"productId\":\"prod-1\"}}");

        verifyNoInteractions(eligibilityPort);
    }

    @Test
    @DisplayName("U45: handleOrderShippedEvent - items boş dizi ise hiçbir kayıt yaratılmaz")
    void handleOrderShippedEvent_WhenItemsArrayIsEmpty_ShouldCreateNothing() {
        listener().handleOrderShippedEvent(
                "{\"orderId\":\"order-1\",\"customerId\":\"cust-1\",\"items\":[]}");

        verifyNoInteractions(eligibilityPort);
    }

    @Test
    @DisplayName("U46: handleOrderShippedEvent - Bozuk JSON kuyruğu bloklamaz, exception yutulur")
    void handleOrderShippedEvent_WithMalformedJson_ShouldSwallowExceptionAndNotThrow() {
        assertThatCode(() -> listener().handleOrderShippedEvent("bu json degil {{"))
                .doesNotThrowAnyException();

        verify(eligibilityPort, never()).createIdempotent(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("U47: handleOrderShippedEvent - Port hata fırlatsa bile listener exception sızdırmaz")
    void handleOrderShippedEvent_WhenPortThrows_ShouldSwallowException() {
        org.mockito.Mockito.doThrow(new IllegalStateException("db down"))
                .when(eligibilityPort).createIdempotent(anyString(), anyString(), anyString());

        assertThatCode(() -> listener().handleOrderShippedEvent(
                "{\"orderId\":\"o\",\"customerId\":\"c\",\"items\":[{\"productId\":\"p\"}]}"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("U48: handleOrderShippedEvent - Eksik productId alanı boş string olarak ele alınır")
    void handleOrderShippedEvent_WhenProductIdMissing_ShouldUseEmptyString() {
        listener().handleOrderShippedEvent(
                "{\"orderId\":\"order-1\",\"customerId\":\"cust-1\",\"items\":[{\"quantity\":1}]}");

        verify(eligibilityPort).createIdempotent("order-1", "cust-1", "");
    }
}
