package com.payment.unit.usecase;

import com.payment.application.port.out.PaymentQueryPort;
import com.payment.application.usecase.PaymentQueryUseCaseImpl;
import com.payment.domain.exception.PaymentNotFoundException;
import com.payment.domain.exception.UnauthorizedPaymentAccessException;
import com.payment.domain.model.Payment;
import com.payment.domain.model.PaymentStatus;
import com.payment.support.PaymentTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT - okuma tarafı. Okuma modeli (Elasticsearch) port arkasında
 * mock'lanır; burada test edilen tek şey yetki kontrolü ve delegasyondur.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - PaymentQueryUseCaseImpl (okuma ve IDOR koruması)")
class PaymentQueryUseCaseImplTest {

    @Mock
    private PaymentQueryPort paymentQueryPort;

    private PaymentQueryUseCaseImpl useCase;

    private UUID customerId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        useCase = new PaymentQueryUseCaseImpl(paymentQueryPort);
        customerId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("U22: getPaymentsByCustomerId - Port'tan gelen liste olduğu gibi döner")
    void getPaymentsByCustomerId_WhenPortReturnsPayments_ShouldReturnThemUnchanged() {
        Payment payment = PaymentTestFixtures.completedPayment(paymentId, customerId, "42.50");
        when(paymentQueryPort.findByCustomerId(customerId)).thenReturn(List.of(payment));

        List<Payment> result = useCase.getPaymentsByCustomerId(customerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("42.50"));
    }

    @Test
    @DisplayName("U23: getPaymentsByCustomerId - Kayıt yoksa boş liste döner (null değil)")
    void getPaymentsByCustomerId_WhenNoPayments_ShouldReturnEmptyList() {
        when(paymentQueryPort.findByCustomerId(customerId)).thenReturn(List.of());

        assertThat(useCase.getPaymentsByCustomerId(customerId)).isEmpty();
    }

    @Test
    @DisplayName("U24: getPaymentsByOrderId - Sipariş kimliğiyle sorgu port'a iletilir")
    void getPaymentsByOrderId_ShouldDelegateToPort() {
        UUID orderId = UUID.randomUUID();
        when(paymentQueryPort.findByOrderId(orderId))
                .thenReturn(List.of(PaymentTestFixtures.payment(paymentId, orderId, customerId, "10.00",
                        PaymentStatus.COMPLETED)));

        assertThat(useCase.getPaymentsByOrderId(orderId)).hasSize(1);
    }

    @Test
    @DisplayName("U25: getAllPayments - Tüm ödemeler port'tan alınır")
    void getAllPayments_ShouldDelegateToPort() {
        when(paymentQueryPort.findAll()).thenReturn(List.of(
                PaymentTestFixtures.completedPayment(UUID.randomUUID(), customerId, "1.00"),
                PaymentTestFixtures.completedPayment(UUID.randomUUID(), customerId, "2.00")));

        assertThat(useCase.getAllPayments()).hasSize(2);
    }

    @Test
    @DisplayName("U26: getPaymentByIdAndCustomerId - Ödeme kullanıcıya aitse döndürülür")
    void getPaymentByIdAndCustomerId_WhenPaymentBelongsToCustomer_ShouldReturnIt() {
        Payment payment = PaymentTestFixtures.completedPayment(paymentId, customerId, "77.77");
        when(paymentQueryPort.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThat(useCase.getPaymentByIdAndCustomerId(paymentId, customerId)).isSameAs(payment);
    }

    @Test
    @DisplayName("U27: getPaymentByIdAndCustomerId - Başkasının ödemesi UnauthorizedPaymentAccessException ile reddedilir (IDOR)")
    void getPaymentByIdAndCustomerId_WhenPaymentBelongsToAnotherCustomer_ShouldThrowUnauthorized() {
        Payment payment = PaymentTestFixtures.completedPayment(paymentId, customerId, "77.77");
        when(paymentQueryPort.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> useCase.getPaymentByIdAndCustomerId(paymentId, UUID.randomUUID()))
                .isInstanceOf(UnauthorizedPaymentAccessException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    @DisplayName("U28: getPaymentByIdAndCustomerId - Ödeme yoksa PaymentNotFoundException fırlatılır")
    void getPaymentByIdAndCustomerId_WhenPaymentDoesNotExist_ShouldThrowNotFound() {
        when(paymentQueryPort.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getPaymentByIdAndCustomerId(paymentId, customerId))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    @DisplayName("U29: getPaymentByIdAndCustomerId - Var olmayan ödeme, yetkisiz erişimden AYRI hata üretir (bilgi sızdırmaz)")
    void getPaymentByIdAndCustomerId_ShouldDistinguishNotFoundFromUnauthorized() {
        when(paymentQueryPort.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getPaymentByIdAndCustomerId(paymentId, customerId))
                .isNotInstanceOf(UnauthorizedPaymentAccessException.class);
    }
}
