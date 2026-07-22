package com.review.infrastructure.search.repository;

import com.review.infrastructure.search.document.ReviewDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewSearchRepository extends ElasticsearchRepository<ReviewDocument, String> {
    List<ReviewDocument> findByProductIdAndStatus(String productId, String status);
    List<ReviewDocument> findByCustomerId(String customerId);
}