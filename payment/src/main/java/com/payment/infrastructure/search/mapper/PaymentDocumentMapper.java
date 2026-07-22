package com.payment.infrastructure.search.mapper;

import com.payment.domain.model.Payment;
import com.payment.domain.model.PaymentStatus;
import com.payment.infrastructure.search.document.PaymentDocument;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentDocumentMapper {

    public Payment toDomain(PaymentDocument document) {
        if (document == null) {
            return null;
        }
        return new Payment(
                // String olan ES ID'sini, Domain'in beklediği UUID formatına çeviriyoruz
                document.getId() != null ? UUID.fromString(document.getId()) : null,
                document.getOrderId(),
                document.getCustomerId(),
                document.getAmount(),
                document.getStatus() != null ? PaymentStatus.valueOf(document.getStatus()) : null
        );
    }

    public PaymentDocument toDocument(Payment domain) {
        if (domain == null) {
            return null;
        }
        PaymentDocument document = new PaymentDocument();

        // Domain'den gelen UUID'yi, ES'nin beklediği String formatına çeviriyoruz
        if (domain.getId() != null) {
            document.setId(domain.getId().toString());
        }

        document.setOrderId(domain.getOrderId());
        document.setCustomerId(domain.getCustomerId());
        document.setAmount(domain.getAmount());

        if (domain.getStatus() != null) {
            document.setStatus(domain.getStatus().name());
        }
        return document;
    }
}