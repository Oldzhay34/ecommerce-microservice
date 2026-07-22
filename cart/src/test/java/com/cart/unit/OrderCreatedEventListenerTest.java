package com.cart.unit;

import com.cart.application.port.in.CartCommandUseCase;
import com.cart.infrastructure.messaging.listener.OrderCreatedEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Katman: UNIT.
 * Hedef: OrderCreatedEvent payload'ının parse guard'ları ve listener'ın
 * idempotency davranışı (aynı mesajın tekrar teslimi patlamamalı).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - OrderCreatedEventListener")
class OrderCreatedEventListenerTest {

    @Mock
    private CartCommandUseCase cartCommandUseCase;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private OrderCreatedEventListener listener;

    @Test
    @DisplayName("U32: handleOrderCreatedEvent - Geçerli payload'da ilgili kullanıcının sepeti temizlenir")
    void handleOrderCreatedEvent_WhenPayloadValid_ShouldClearCartOfUser() {
        UUID userId = UUID.randomUUID();
        String payload = "{\"id\":\"order-1\",\"userId\":\"" + userId + "\",\"status\":\"PENDING\"}";

        listener.handleOrderCreatedEvent(payload);

        verify(cartCommandUseCase).clearCart(userId);
    }

    @Test
    @DisplayName("U33: handleOrderCreatedEvent - Aynı mesaj iki kez teslim edilirse hata olmadan tekrar işlenir (idempotency)")
    void handleOrderCreatedEvent_WhenSameMessageDeliveredTwice_ShouldStayIdempotent() {
        UUID userId = UUID.randomUUID();
        String payload = "{\"userId\":\"" + userId + "\"}";

        assertThatCode(() -> {
            listener.handleOrderCreatedEvent(payload);
            listener.handleOrderCreatedEvent(payload);
        }).doesNotThrowAnyException();

        verify(cartCommandUseCase, times(2)).clearCart(userId);
    }

    @Test
    @DisplayName("U34: handleOrderCreatedEvent - userId alanı yoksa RuntimeException fırlatır ve sepete dokunmaz")
    void handleOrderCreatedEvent_WhenUserIdMissing_ShouldThrowAndNotTouchCart() {
        assertThatThrownBy(() -> listener.handleOrderCreatedEvent("{\"id\":\"order-1\"}"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process OrderCreatedEvent");

        verifyNoInteractions(cartCommandUseCase);
    }

    @Test
    @DisplayName("U35: handleOrderCreatedEvent - userId geçerli bir UUID değilse RuntimeException fırlatır")
    void handleOrderCreatedEvent_WhenUserIdIsNotUuid_ShouldThrow() {
        assertThatThrownBy(() -> listener.handleOrderCreatedEvent("{\"userId\":\"not-a-uuid\"}"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process OrderCreatedEvent");

        verifyNoInteractions(cartCommandUseCase);
    }

    @Test
    @DisplayName("U36: handleOrderCreatedEvent - Bozuk JSON gelirse RuntimeException fırlatır")
    void handleOrderCreatedEvent_WhenPayloadIsMalformedJson_ShouldThrow() {
        assertThatThrownBy(() -> listener.handleOrderCreatedEvent("{not-json"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process OrderCreatedEvent");

        verifyNoInteractions(cartCommandUseCase);
    }
}
