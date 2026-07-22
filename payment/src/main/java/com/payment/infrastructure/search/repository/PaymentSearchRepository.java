package com.payment.infrastructure.search.repository;

import com.payment.infrastructure.search.document.PaymentDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
// DİKKAT: İkinci parametre UUID'den String'e çevrildi!
public interface PaymentSearchRepository extends ElasticsearchRepository<PaymentDocument, String> {

    // Müşteri ve Sipariş ID'leri Document içinde hala UUID kaldığı için bu metotlar bozulmaz, böyle kalabilir.
    List<PaymentDocument> findByCustomerId(UUID customerId);
    List<PaymentDocument> findByOrderId(UUID orderId);
}