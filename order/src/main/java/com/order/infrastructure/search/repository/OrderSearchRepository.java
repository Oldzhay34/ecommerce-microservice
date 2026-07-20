package com.order.infrastructure.search.repository;

import com.order.infrastructure.search.document.OrderDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderSearchRepository extends ElasticsearchRepository<OrderDocument, String> {
    List<OrderDocument> findByUserId(String userId);
    List<OrderDocument> findByItemsStoreId(String storeId);
}