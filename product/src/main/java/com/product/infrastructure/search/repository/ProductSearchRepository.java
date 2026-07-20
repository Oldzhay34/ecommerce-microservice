package com.product.infrastructure.search.repository;

import com.product.infrastructure.search.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, UUID> {
    // Adapter sınıfında ElasticsearchTemplate kullandık ancak
    // Spring Data Elasticsearch'ün repository yetenekleri kullanılmak istenirse bu interface yeterlidir.
    List<ProductDocument> findByNameContaining(String keyword);
}