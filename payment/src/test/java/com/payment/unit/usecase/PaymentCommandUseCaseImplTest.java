package com.payment.unit.usecase;

import com.payment.application.port.out.PaymentChargeCommand;
import com.payment.application.port.out.PaymentCommandPort;
import com.payment.application.port.out.PaymentGatewayPort;
import com.payment.application.port.out.PaymentQueryPort;
import com.payment.application.usecase.PaymentCommandUseCaseImpl;
import com.payment.domain.exception.InvalidRefundException;
import com.payment.domain.exception.PaymentGatewayNotConfiguredException;
import com.payment.domain.exception.PaymentNotFoundException;
import com.payment.domain.model.Payment;
import com.payment.domain.model.PaymentStatus;
import com.payment.support.PaymentTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT - saf Mockito, hiçbir altyapı yok.
 * <p>
 * Hexagonal mimari gereği use case yalnızca port'lara bağlıdır; üç çıkış port'u da
 * ({@link PaymentCommandPort}, {@link PaymentQueryPort}, {@link PaymentGatewayPort})
 * mock'lanır. Para hesapları {@code isEqualByComparingTo} ile karşılaştırılır:
 * {@code BigDecimal.equals} ÖLÇEĞİ de karşılaştırdığı için 100 ile 100.00'ı farklı sayar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - PaymentCommandUseCaseImpl (tahsilat, iade guard'ları, idempotency)")
class PaymentCommandUseCaseImplTest {

    @Mock
    private PaymentCommandPort paymentCommandPort;

    @Mock
    private PaymentQueryPort paymentQueryPort;

    @Mock
    private PaymentGatewayPort paymentGatewayPort;

    private PaymentCommandUseCaseImpl useCase;

    private UUID orderId;
    private UUID customerId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        useCase = new PaymentCommandUseCaseImpl(paymentCommandPort, paymentQueryPort, paymentGatewayPort);
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
    }

    /** İlk save'de DB'nin ID atamasını taklit eder (ID uygulama tarafından atanmaz). */
    private void stubSaveAssigningId() {
        when(paymentCommandPort.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment argument = invocation.getArgument(0);
            if (argument.getId() == null) {
                argument.setId(paymentId);
            }
            return argument;
        });
    }

    // ==================== processOrderApproved ====================

    @Nested
    @DisplayName("processOrderApproved - order.approved olayı üzerine tahsilat")
    class ProcessOrderApproved {

        @Test
        @DisplayName("U1: processOrderApproved - Gateway başarılı olduğunda ödeme COMPLETED kaydedilir ve PaymentCompletedEvent yazılır")
        void processOrderApproved_WhenGatewaySucceeds_ShouldPersistCompletedAndEmitCompletedEvent() {
            when(paymentCommandPort.existsPaymentForOrder(orderId)).thenReturn(false);
            stubSaveAssigningId();
            when(paymentGatewayPort.charge(any())).thenReturn(PaymentTestFixtures.completedResult());

            useCase.processOrderApproved(orderId, customerId, new BigDecimal("250.75"));

            ArgumentCaptor<Payment> savedCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentCommandPort, times(2)).save(savedCaptor.capture());
            assertThat(savedCaptor.getAllValues().get(1).getStatus()).isEqualTo(PaymentStatus.COMPLETED);

            verify(paymentCommandPort).saveOutboxEvent(eq("Payment"), eq(paymentId.toString()),
                    eq("PaymentCompletedEvent"), anyString());
        }

        @Test
        @DisplayName("U2: processOrderApproved - Gateway reddettiğinde ödeme FAILED kaydedilir ve PaymentFailedEvent yazılır")
        void processOrderApproved_WhenGatewayDeclines_ShouldPersistFailedAndEmitFailedEvent() {
            when(paymentCommandPort.existsPaymentForOrder(orderId)).thenReturn(false);
            stubSaveAssigningId();
            when(paymentGatewayPort.charge(any())).thenReturn(PaymentTestFixtures.failedResult());

            useCase.processOrderApproved(orderId, customerId, new BigDecimal("999999.99"));

            ArgumentCaptor<Payment> savedCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentCommandPort, times(2)).save(savedCaptor.capture());
            assertThat(savedCaptor.getAllValues().get(1).getStatus()).isEqualTo(PaymentStatus.FAILED);

            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
            verify(paymentCommandPort).saveOutboxEvent(anyString(), anyString(), eq("PaymentFailedEvent"),
                    payloadCaptor.capture());
            assertThat(payloadCaptor.getValue()).contains("\"status\":\"FAILED\"");
        }

        @Test
        @DisplayName("U3: processOrderApproved - Ödeme önce PENDING kaydedilir, gateway ANCAK ondan sonra çağrılır")
        void processOrderApproved_ShouldPersistPendingBeforeCallingGateway() {
            when(paymentCommandPort.existsPaymentForOrder(orderId)).thenReturn(false);
            ArgumentCaptor<Payment> firstSave = ArgumentCaptor.forClass(Payment.class);
            when(paymentCommandPort.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment argument = invocation.getArgument(0);
                if (argument.getId() == null) {
                    // gateway çağrısından ÖNCEKİ durumu doğrulayabilmek için kopya al
                    assertThat(argument.getStatus()).isEqualTo(PaymentStatus.PENDING);
                    argument.setId(paymentId);
                }
                return argument;
            });
            when(paymentGatewayPort.charge(any())).thenReturn(PaymentTestFixtures.completedResult());

            useCase.processOrderApproved(orderId, customerId, PaymentTestFixtures.TRY_100);

            InOrder order = inOrder(paymentCommandPort, paymentGatewayPort);
            order.verify(paymentCommandPort).save(firstSave.capture());
            order.verify(paymentGatewayPort).charge(any());
            order.verify(paymentCommandPort).save(any(Payment.class));
        }

        @Test
        @DisplayName("U4: processOrderApproved - Gateway'e giden tutar KURUŞU KURUŞUNA aynıdır ve para birimi TRY'dir")
        void processOrderApproved_ShouldForwardExactAmountAndTryCurrencyToGateway() {
            when(paymentCommandPort.existsPaymentForOrder(orderId)).thenReturn(false);
            stubSaveAssigningId();
            when(paymentGatewayPort.charge(any())).thenReturn(PaymentTestFixtures.completedResult());

            BigDecimal amount = new BigDecimal("1234.56");
            useCase.processOrderApproved(orderId, customerId, amount);

            ArgumentCaptor<PaymentChargeCommand> commandCaptor = ArgumentCaptor.forClass(PaymentChargeCommand.class);
            verify(paymentGatewayPort).charge(commandCaptor.capture());

            PaymentChargeCommand command = commandCaptor.getValue();
            assertThat(command.amount()).isEqualByComparingTo(new BigDecimal("1234.56"));
            assertThat(command.amount().scale()).as("kuruş hassasiyeti korunmalı").isEqualTo(2);
            assertThat(command.currency()).isEqualTo("TRY");
            assertThat(command.orderId()).isEqualTo(orderId);
            assertThat(command.customerId()).isEqualTo(customerId);
        }

        @Test
        @DisplayName("U5: processOrderApproved - Tutar hiçbir noktada yuvarlanmaz veya yeniden ölçeklenmez")
        void processOrderApproved_ShouldNotRoundOrRescaleAmount() {
            when(paymentCommandPort.existsPaymentForOrder(orderId)).thenReturn(false);
            stubSaveAssigningId();
            when(paymentGatewayPort.charge(any())).thenReturn(PaymentTestFixtures.completedResult());

            BigDecimal awkward = new BigDecimal("0.005");
            useCase.processOrderApproved(orderId, customerId, awkward);

            ArgumentCaptor<Payment> savedCaptor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentCommandPort, times(2)).save(savedCaptor.capture());
            assertThat(savedCaptor.getAllValues().get(0).getAmount())
                    .isEqualByComparingTo(new BigDecimal("0.005"));
            assertThat(savedCaptor.getAllValues().get(0).getAmount().unscaledValue().intValue()).isEqualTo(5);
        }

        /**
         * REGRESYON: Bu kontrol production kodunda YOKTU. RabbitMQ en-az-bir-kez teslimat
         * garantisi verdiği için tekrar teslim edilen order.approved olayı ikinci bir
         * tahsilata yol açıyordu (müşteriden iki kez para çekilmesi).
         */
        @Test
        @DisplayName("U6: processOrderApproved - Sipariş için ödeme zaten varsa gateway HİÇ çağrılmaz (idempotency)")
        void processOrderApproved_WhenPaymentAlreadyExistsForOrder_ShouldNotChargeAgain() {
            when(paymentCommandPort.existsPaymentForOrder(orderId)).thenReturn(true);

            useCase.processOrderApproved(orderId, customerId, PaymentTestFixtures.TRY_100);

            verifyNoInteractions(paymentGatewayPort);
            verify(paymentCommandPort, never()).save(any());
            verify(paymentCommandPort, never()).saveOutboxEvent(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("U7: processOrderApproved - Aynı olay iki kez işlenirse toplam tahsilat sayısı 1 olur")
        void processOrderApproved_WhenSameEventDeliveredTwice_ShouldChargeExactlyOnce() {
            when(paymentCommandPort.existsPaymentForOrder(orderId)).thenReturn(false, true);
            stubSaveAssigningId();
            when(paymentGatewayPort.charge(any())).thenReturn(PaymentTestFixtures.completedResult());

            useCase.processOrderApproved(orderId, customerId, PaymentTestFixtures.TRY_100);
            useCase.processOrderApproved(orderId, customerId, PaymentTestFixtures.TRY_100);

            verify(paymentGatewayPort, times(1)).charge(any());
            verify(paymentCommandPort, times(1))
                    .saveOutboxEvent(anyString(), anyString(), eq("PaymentCompletedEvent"), anyString());
        }

        @Test
        @DisplayName("U8: processOrderApproved - Gateway ağ hatası fırlatırsa hata yukarı taşınır ve outbox olayı YAZILMAZ")
        void processOrderApproved_WhenGatewayThrows_ShouldPropagateAndNotEmitOutboxEvent() {
            when(paymentCommandPort.existsPaymentForOrder(orderId)).thenReturn(false);
            stubSaveAssigningId();
            when(paymentGatewayPort.charge(any()))
                    .thenThrow(new PaymentGatewayNotConfiguredException("gateway down"));

            assertThatThrownBy(() -> useCase.processOrderApproved(orderId, customerId, PaymentTestFixtures.TRY_100))
                    .isInstanceOf(PaymentGatewayNotConfiguredException.class);

            // @Transactional geri alacağı için tek yazma bile kalıcı olmaz; kritik olan
            // COMPLETED durumunun ve olayının ASLA üretilmemesidir.
            verify(paymentCommandPort, never()).saveOutboxEvent(anyString(), anyString(), anyString(), anyString());
            verify(paymentCommandPort, times(1)).save(any(Payment.class));
        }

        @Test
        @DisplayName("U9: processOrderApproved - Outbox olayı aggregate tipi/kimliği ile birlikte yazılır")
        void processOrderApproved_ShouldWriteOutboxEventWithAggregateIdentity() {
            when(paymentCommandPort.existsPaymentForOrder(orderId)).thenReturn(false);
            stubSaveAssigningId();
            when(paymentGatewayPort.charge(any())).thenReturn(PaymentTestFixtures.completedResult());

            useCase.processOrderApproved(orderId, customerId, PaymentTestFixtures.TRY_100);

            ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
            verify(paymentCommandPort).saveOutboxEvent(eq("Payment"), eq(paymentId.toString()),
                    eq("PaymentCompletedEvent"), payload.capture());
            assertThat(payload.getValue())
                    .contains(paymentId.toString())
                    .contains(orderId.toString())
                    .contains("COMPLETED");
        }
    }

    // ==================== requestRefund ====================

    @Nested
    @DisplayName("requestRefund - müşterinin iade talebi")
    class RequestRefund {

        @Test
        @DisplayName("U10: requestRefund - Ödeme bulunamazsa PaymentNotFoundException fırlatılır")
        void requestRefund_WhenPaymentDoesNotExist_ShouldThrowPaymentNotFound() {
            when(paymentQueryPort.findById(paymentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.requestRefund(paymentId, customerId, "cayma hakkı"))
                    .isInstanceOf(PaymentNotFoundException.class);

            verify(paymentCommandPort, never()).save(any());
        }

        /**
         * IDOR: başka bir müşterinin ödemesi için iade talebi reddedilmelidir.
         * NOT: Production kodu burada UnauthorizedPaymentAccessException değil
         * InvalidRefundException fırlatıyor (sorgu tarafında ise Unauthorized...).
         * Erişim engellendiği için güvenlik açığı yok; test GERÇEK davranışı sabitler.
         */
        @Test
        @DisplayName("U11: requestRefund - Başka müşterinin ödemesi için talep reddedilir (IDOR)")
        void requestRefund_WhenPaymentBelongsToAnotherCustomer_ShouldBeRejected() {
            UUID attacker = UUID.randomUUID();
            when(paymentQueryPort.findById(paymentId))
                    .thenReturn(Optional.of(PaymentTestFixtures.completedPayment(paymentId, customerId, "100.00")));

            assertThatThrownBy(() -> useCase.requestRefund(paymentId, attacker, "istemedim"))
                    .isInstanceOf(InvalidRefundException.class)
                    .hasMessageContaining("your own payments");

            verify(paymentCommandPort, never()).save(any());
            verify(paymentCommandPort, never()).saveOutboxEvent(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("U12: requestRefund - COMPLETED ödeme REFUND_REQUESTED olur ve RefundRequestedEvent yazılır")
        void requestRefund_WhenPaymentIsCompleted_ShouldMoveToRefundRequestedAndEmitEvent() {
            Payment payment = PaymentTestFixtures.completedPayment(paymentId, customerId, "100.00");
            when(paymentQueryPort.findById(paymentId)).thenReturn(Optional.of(payment));

            useCase.requestRefund(paymentId, customerId, "ürün hasarlı geldi");

            ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
            verify(paymentCommandPort).save(saved.capture());
            assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.REFUND_REQUESTED);
            assertThat(saved.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));

            ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
            verify(paymentCommandPort).saveOutboxEvent(eq("Payment"), eq(paymentId.toString()),
                    eq("RefundRequestedEvent"), payload.capture());
            assertThat(payload.getValue()).contains("ürün hasarlı geldi");
        }

        /**
         * InvalidRefundException'ın durum kaynaklı TÜM tetiklenme yolları.
         * COMPLETED dışındaki her durum reddedilmelidir - buna
         * "aynı ödemenin ikinci kez iadesi" (REFUNDED) ve "aynı talebin tekrarı"
         * (REFUND_REQUESTED) da dahildir.
         */
        @ParameterizedTest(name = "durum={0}")
        @EnumSource(value = PaymentStatus.class, names = {"PENDING", "FAILED", "REFUND_REQUESTED", "REFUNDED"})
        @DisplayName("U13: requestRefund - COMPLETED olmayan her durumda InvalidRefundException fırlatılır")
        void requestRefund_WhenStatusIsNotCompleted_ShouldThrowInvalidRefund(PaymentStatus status) {
            Payment payment = PaymentTestFixtures.payment(paymentId, orderId, customerId, "100.00", status);
            when(paymentQueryPort.findById(paymentId)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> useCase.requestRefund(paymentId, customerId, "sebep"))
                    .isInstanceOf(InvalidRefundException.class)
                    .hasMessageContaining("Only COMPLETED payments can be refunded");

            assertThat(payment.getStatus()).as("reddedilen talep durumu DEĞİŞTİRMEMELİ").isEqualTo(status);
            verify(paymentCommandPort, never()).save(any());
            verify(paymentCommandPort, never()).saveOutboxEvent(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("U14: requestRefund - Aynı ödeme için ikinci iade talebi reddedilir (çift iade koruması)")
        void requestRefund_WhenCalledTwice_ShouldRejectSecondAttempt() {
            Payment payment = PaymentTestFixtures.completedPayment(paymentId, customerId, "100.00");
            when(paymentQueryPort.findById(paymentId)).thenReturn(Optional.of(payment));

            useCase.requestRefund(paymentId, customerId, "ilk talep");

            assertThatThrownBy(() -> useCase.requestRefund(paymentId, customerId, "ikinci talep"))
                    .isInstanceOf(InvalidRefundException.class);

            verify(paymentCommandPort, times(1)).save(any());
        }

        @Test
        @DisplayName("U15: requestRefund - Sebepteki çift tırnaklar kaçırılır, outbox payload'ı bozulmaz")
        void requestRefund_WhenReasonContainsQuotes_ShouldEscapeThemInPayload() {
            when(paymentQueryPort.findById(paymentId))
                    .thenReturn(Optional.of(PaymentTestFixtures.completedPayment(paymentId, customerId, "100.00")));

            useCase.requestRefund(paymentId, customerId, "kutu \"ezik\" geldi");

            ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
            verify(paymentCommandPort).saveOutboxEvent(anyString(), anyString(), anyString(), payload.capture());
            assertThat(payload.getValue()).contains("\\\"ezik\\\"");
        }

        @Test
        @DisplayName("U16: requestRefund - Sebep null ise payload boş sebeple üretilir, NPE oluşmaz")
        void requestRefund_WhenReasonIsNull_ShouldProduceEmptyReasonWithoutNpe() {
            when(paymentQueryPort.findById(paymentId))
                    .thenReturn(Optional.of(PaymentTestFixtures.completedPayment(paymentId, customerId, "100.00")));

            useCase.requestRefund(paymentId, customerId, null);

            ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
            verify(paymentCommandPort).saveOutboxEvent(anyString(), anyString(), anyString(), payload.capture());
            assertThat(payload.getValue()).contains("\"reason\":\"\"");
        }
    }

    // ==================== approveRefund ====================

    @Nested
    @DisplayName("approveRefund - admin onayı ile iadenin gerçekleşmesi")
    class ApproveRefund {

        @Test
        @DisplayName("U17: approveRefund - Ödeme bulunamazsa PaymentNotFoundException fırlatılır")
        void approveRefund_WhenPaymentDoesNotExist_ShouldThrowPaymentNotFound() {
            when(paymentQueryPort.findById(paymentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.approveRefund(paymentId))
                    .isInstanceOf(PaymentNotFoundException.class);

            verify(paymentCommandPort, never()).save(any());
        }

        @Test
        @DisplayName("U18: approveRefund - REFUND_REQUESTED ödeme REFUNDED olur ve PaymentRefundedEvent yazılır")
        void approveRefund_WhenRefundRequested_ShouldMoveToRefundedAndEmitEvent() {
            Payment payment = PaymentTestFixtures.payment(paymentId, orderId, customerId, "100.00",
                    PaymentStatus.REFUND_REQUESTED);
            when(paymentQueryPort.findById(paymentId)).thenReturn(Optional.of(payment));

            useCase.approveRefund(paymentId);

            ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
            verify(paymentCommandPort).save(saved.capture());
            assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(saved.getValue().getAmount())
                    .as("iade tutarı ödenen tutarla birebir aynı olmalı")
                    .isEqualByComparingTo(new BigDecimal("100.00"));

            verify(paymentCommandPort).saveOutboxEvent(eq("Payment"), eq(paymentId.toString()),
                    eq("PaymentRefundedEvent"), anyString());
        }

        /**
         * Admin onayının, talep aşamasından geçmeyen ödemeleri iade etmesi engellenir.
         * REFUNDED durumu buradaki en kritik satırdır: aynı ödeme İKİ KEZ iade edilemez.
         */
        @ParameterizedTest(name = "durum={0}")
        @EnumSource(value = PaymentStatus.class, names = {"PENDING", "COMPLETED", "FAILED", "REFUNDED"})
        @DisplayName("U19: approveRefund - REFUND_REQUESTED olmayan her durumda InvalidRefundException fırlatılır")
        void approveRefund_WhenStatusIsNotRefundRequested_ShouldThrowInvalidRefund(PaymentStatus status) {
            Payment payment = PaymentTestFixtures.payment(paymentId, orderId, customerId, "100.00", status);
            when(paymentQueryPort.findById(paymentId)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> useCase.approveRefund(paymentId))
                    .isInstanceOf(InvalidRefundException.class)
                    .hasMessageContaining("pending refund request");

            assertThat(payment.getStatus()).isEqualTo(status);
            verify(paymentCommandPort, never()).save(any());
            verify(paymentCommandPort, never()).saveOutboxEvent(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("U20: approveRefund - Aynı ödeme ikinci kez onaylanamaz, tek bir iade olayı üretilir")
        void approveRefund_WhenCalledTwice_ShouldRefundOnlyOnce() {
            Payment payment = PaymentTestFixtures.payment(paymentId, orderId, customerId, "100.00",
                    PaymentStatus.REFUND_REQUESTED);
            when(paymentQueryPort.findById(paymentId)).thenReturn(Optional.of(payment));

            useCase.approveRefund(paymentId);

            assertThatThrownBy(() -> useCase.approveRefund(paymentId))
                    .isInstanceOf(InvalidRefundException.class);

            verify(paymentCommandPort, times(1))
                    .saveOutboxEvent(anyString(), anyString(), eq("PaymentRefundedEvent"), anyString());
        }

        @Test
        @DisplayName("U21: approveRefund - Gateway'e dokunulmaz (iade akışı tahsilat yapmaz)")
        void approveRefund_ShouldNotTouchPaymentGateway() {
            when(paymentQueryPort.findById(paymentId)).thenReturn(Optional.of(
                    PaymentTestFixtures.payment(paymentId, orderId, customerId, "100.00",
                            PaymentStatus.REFUND_REQUESTED)));

            useCase.approveRefund(paymentId);

            verifyNoInteractions(paymentGatewayPort);
        }
    }
}
