package com.payment.infrastructure.persistence.mapper;

import com.payment.domain.model.Payment;
import com.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentEntityMapper {

    public Payment toDomain(PaymentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Payment(
                entity.getId(),
                entity.getOrderId(),
                entity.getCustomerId(),
                entity.getAmount(),
                entity.getStatus() // Artık dönüştürmeye gerek yok, ikisi de PaymentStatus tipinde
        );
    }

    public PaymentJpaEntity toEntity(Payment domain) {
        if (domain == null) {
            return null;
        }
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(domain.getId());
        entity.setOrderId(domain.getOrderId());
        entity.setCustomerId(domain.getCustomerId());
        entity.setAmount(domain.getAmount());

        // Artık .name() diyerek String'e çevirmeye gerek yok
        entity.setStatus(domain.getStatus());

        return entity;
    }
}