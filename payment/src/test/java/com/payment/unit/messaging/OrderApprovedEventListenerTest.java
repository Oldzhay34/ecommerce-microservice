package com.payment.unit.messaging;

import com.payment.application.port.in.PaymentCommandUseCase;
import com.payment.infrastructure.messaging.listener.OrderApprovedEventListener;
import com.payment.support.PaymentTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Katman: UNIT - order.approved dinleyicisi.
 * <p>
 * Dinleyici payload'ı regex ile ayrıştırır ve hataları yutar (DLQ henüz yok).
 * Buradaki kritik nokta: bozuk payload kuyruğu tıkamamalı, ancak GEÇERSİZ bir
 * olay da SESSİZCE tahsilata dönüşmemelidir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - OrderApprovedEventListener (payload ayrıştırma ve idempotency)")
class OrderApprovedEventListenerTest {

    @Mock
    private PaymentCommandUseCase paymentCommandUseCase;

    private OrderApprovedEventListener listener;

    private UUID orderId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        listener = new OrderApprovedEventListener(paymentCommandUseCase);
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("U62: handleOrderApprovedEvent - Geçerli payload'dan sipariş, müşteri ve tutar doğru çıkarılır")
    void handleOrderApprovedEvent_WhenPayloadIsValid_ShouldExtractAllFields() {
        listener.handleOrderApprovedEvent(
                PaymentTestFixtures.orderApprovedPayload(orderId, customerId, "1234.56"));

        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(paymentCommandUseCase).processOrderApproved(eq(orderId), eq(customerId), amountCaptor.capture());
        assertThat(amountCaptor.getValue()).isEqualByComparingTo(new BigDecimal("1234.56"));
        assertThat(amountCaptor.getValue().scale())
                .as("kuruşlar payload'dan okunurken kaybolmamalı").isEqualTo(2);
    }

    @Test
    @DisplayName("U63: handleOrderApprovedEvent - Tam sayı tutar da BigDecimal olarak okunur")
    void handleOrderApprovedEvent_WhenAmountHasNoDecimals_ShouldStillParse() {
        listener.handleOrderApprovedEvent(
                PaymentTestFixtures.orderApprovedPayload(orderId, customerId, "500"));

        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(paymentCommandUseCase).processOrderApproved(any(), any(), amountCaptor.capture());
        assertThat(amountCaptor.getValue()).isEqualByComparingTo(new BigDecimal("500"));
    }

    /**
     * KRİTİK: Aynı olay iki kez teslim edildiğinde use case'e iki çağrı gider,
     * ancak tahsilat idempotency'si use case katmanında korunur
     * (bkz. PaymentCommandUseCaseImplTest U6/U7). Dinleyici seviyesinde
     * doğrulanan şey, ikinci teslimatın da AYNI parametrelerle iletilmesi ve
     * dinleyicinin kendi başına ek bir tahsilat üretmemesidir.
     */
    @Test
    @DisplayName("U64: handleOrderApprovedEvent - Aynı olay iki kez gelirse use case aynı parametrelerle çağrılır (tekilleştirme use case'de)")
    void handleOrderApprovedEvent_WhenSameEventDeliveredTwice_ShouldForwardIdenticalCommand() {
        String payload = PaymentTestFixtures.orderApprovedPayload(orderId, customerId, "100.00");

        listener.handleOrderApprovedEvent(payload);
        listener.handleOrderApprovedEvent(payload);

        verify(paymentCommandUseCase, times(2))
                .processOrderApproved(eq(orderId), eq(customerId), any(BigDecimal.class));
    }

    @ParameterizedTest(name = "payload={0}")
    @ValueSource(strings = {
            "",
            "not-json-at-all",
            "{}",
            "{\"customerId\":\"11111111-1111-1111-1111-111111111111\", \"amount\":10.00}",
            "{\"orderId\":\"11111111-1111-1111-1111-111111111111\", \"amount\":10.00}",
            "{\"orderId\":\"11111111-1111-1111-1111-111111111111\", \"customerId\":\"22222222-2222-2222-2222-222222222222\"}",
            "{\"orderId\":\"not-a-uuid\", \"customerId\":\"22222222-2222-2222-2222-222222222222\", \"amount\":10.00}"
    })
    @DisplayName("U65: handleOrderApprovedEvent - Eksik/bozuk payload tahsilata dönüşmez ve dinleyici patlamaz")
    void handleOrderApprovedEvent_WhenPayloadIsMalformed_ShouldNotChargeAndShouldNotThrow(String payload) {
        assertThatCode(() -> listener.handleOrderApprovedEvent(payload)).doesNotThrowAnyException();

        verifyNoInteractions(paymentCommandUseCase);
    }

    @Test
    @DisplayName("U66: handleOrderApprovedEvent - Use case hata fırlatırsa dinleyici hatayı yutar (kuyruk tıkanmaz)")
    void handleOrderApprovedEvent_WhenUseCaseThrows_ShouldSwallowExceptionToAvoidPoisonMessage() {
        doThrow(new IllegalStateException("db down"))
                .when(paymentCommandUseCase).processOrderApproved(any(), any(), any());

        assertThatCode(() -> listener.handleOrderApprovedEvent(
                PaymentTestFixtures.orderApprovedPayload(orderId, customerId, "10.00")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("U67: handleOrderApprovedEvent - Negatif tutarlı olay tahsilata dönüşmez")
    void handleOrderApprovedEvent_WhenAmountIsNegative_ShouldNotCharge() {
        // Tutar regex'i yalnızca [0-9.] eşler; "-50.00" hiç eşleşmez ve olay
        // ayrıştırma hatasıyla düşer. Böylece negatif tutar ASLA gateway'e gitmez.
        listener.handleOrderApprovedEvent(
                "{\"orderId\":\"" + orderId + "\", \"customerId\":\"" + customerId + "\", \"amount\":-50.00}");

        verifyNoInteractions(paymentCommandUseCase);
    }

    @Test
    @DisplayName("U68: handleOrderApprovedEvent - null payload tahsilat üretmez")
    void handleOrderApprovedEvent_WhenPayloadIsNull_ShouldNotCharge() {
        assertThatCode(() -> listener.handleOrderApprovedEvent(null)).doesNotThrowAnyException();

        verify(paymentCommandUseCase, never()).processOrderApproved(any(), any(), any());
    }
}
