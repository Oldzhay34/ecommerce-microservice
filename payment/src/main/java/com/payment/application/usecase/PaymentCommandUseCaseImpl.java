package com.payment.application.usecase;

import com.payment.application.port.in.PaymentCommandUseCase;
import com.payment.application.port.out.PaymentCommandPort;
import com.payment.application.port.out.PaymentGatewayPort;
import com.payment.application.port.out.PaymentChargeCommand;
import com.payment.application.port.out.PaymentGatewayResult;
import com.payment.application.port.out.PaymentQueryPort;
import com.payment.domain.exception.InvalidRefundException;
import com.payment.domain.exception.PaymentNotFoundException;
import com.payment.domain.model.Payment;
import com.payment.domain.model.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentCommandUseCaseImpl implements PaymentCommandUseCase {

    private final PaymentCommandPort paymentCommandPort;
    private final PaymentQueryPort paymentQueryPort;
    private final PaymentGatewayPort paymentGatewayPort;

    public PaymentCommandUseCaseImpl(PaymentCommandPort paymentCommandPort,
                                     PaymentQueryPort paymentQueryPort,
                                     PaymentGatewayPort paymentGatewayPort) {
        this.paymentCommandPort = paymentCommandPort;
        this.paymentQueryPort = paymentQueryPort;
        this.paymentGatewayPort = paymentGatewayPort;
    }

    @Override
    @Transactional
    public void processOrderApproved(UUID orderId, UUID customerId, BigDecimal amount) {
        // BUG DÜZELTMESİ (idempotency): Bu kontrol yoktu. RabbitMQ en-az-bir-kez
        // teslimat garantisi verir; aynı order.approved olayı yeniden teslim
        // edildiğinde ikinci bir Payment kaydı açılıp gateway'e İKİNCİ KEZ tahsilat
        // yapılıyordu (müşteriden iki kez para çekilmesi). Kontrol, near-real-time
        // olan Elasticsearch okuma modeli üzerinden değil, güçlü tutarlı yazma
        // modeli üzerinden yapılır.
        if (paymentCommandPort.existsPaymentForOrder(orderId)) {
            return;
        }

        Payment payment = new Payment(null, orderId, customerId, amount, PaymentStatus.PENDING);
        payment = paymentCommandPort.save(payment);

        PaymentChargeCommand chargeCommand = new PaymentChargeCommand(orderId, customerId, amount, "TRY");
        PaymentGatewayResult result = paymentGatewayPort.charge(chargeCommand);

        payment.setStatus(result.status());
        paymentCommandPort.save(payment);

        String eventType = result.status() == PaymentStatus.COMPLETED ? "PaymentCompletedEvent" : "PaymentFailedEvent";
        String payload = String.format("{\"paymentId\":\"%s\", \"orderId\":\"%s\", \"status\":\"%s\"}",
                payment.getId(), orderId, result.status().name());

        paymentCommandPort.saveOutboxEvent("Payment", payment.getId().toString(), eventType, payload);
    }

    @Override
    @Transactional
    public void requestRefund(UUID paymentId, UUID customerId, String reason) {
        Payment payment = paymentQueryPort.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (!payment.getCustomerId().equals(customerId)) {
            throw new InvalidRefundException("You can only request a refund for your own payments.");
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidRefundException("Only COMPLETED payments can be refunded. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUND_REQUESTED);
        paymentCommandPort.save(payment);

        String payload = String.format("{\"paymentId\":\"%s\", \"orderId\":\"%s\", \"status\":\"%s\", \"reason\":\"%s\"}",
                payment.getId(), payment.getOrderId(), PaymentStatus.REFUND_REQUESTED.name(),
                reason != null ? reason.replace("\"", "\\\"") : "");

        paymentCommandPort.saveOutboxEvent("Payment", payment.getId().toString(), "RefundRequestedEvent", payload);
    }

    @Override
    @Transactional
    public void approveRefund(UUID paymentId) {
        Payment payment = paymentQueryPort.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.REFUND_REQUESTED) {
            throw new InvalidRefundException("Only payments with a pending refund request can be approved. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentCommandPort.save(payment);

        String payload = String.format("{\"paymentId\":\"%s\", \"orderId\":\"%s\", \"status\":\"%s\"}",
                payment.getId(), payment.getOrderId(), PaymentStatus.REFUNDED.name());

        paymentCommandPort.saveOutboxEvent("Payment", payment.getId().toString(), "PaymentRefundedEvent", payload);
    }
}