package com.product.infrastructure.search.adapter;

import com.product.application.port.out.ProductQueryPort;
import com.product.domain.model.Product;
import com.product.infrastructure.search.document.ProductDocument;
import com.product.infrastructure.search.mapper.ProductDocumentMapper;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProductSearchAdapter implements ProductQueryPort {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ProductDocumentMapper documentMapper;

    public ProductSearchAdapter(ElasticsearchTemplate elasticsearchTemplate, ProductDocumentMapper documentMapper) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.documentMapper = documentMapper;
    }

    @Override
    public List<Product> searchProducts(String keyword, String category) {
        Criteria criteria = new Criteria();
        if (keyword != null && !keyword.isBlank()) {
            criteria = criteria.and(new Criteria("name").contains(keyword));
        }
        if (category != null && !category.isBlank()) {
            criteria = criteria.and(new Criteria("category").is(category));
        }

        CriteriaQuery query = new CriteriaQuery(criteria);
        SearchHits<ProductDocument> hits = elasticsearchTemplate.search(query, ProductDocument.class);

        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(documentMapper::toDomain)
                .collect(Collectors.toList());
    }

    // YENİ EKLENEN METOT
    @Override
    public Optional<Product> findById(UUID id) {
        // Elasticsearch'ten ID ile dökümanı getir (genellikle Elasticsearch ID'leri String olarak yönetilir)
        ProductDocument document = elasticsearchTemplate.get(id.toString(), ProductDocument.class);

        // Eğer döküman bulunduysa (null değilse) domain nesnesine (Product) dönüştür
        return Optional.ofNullable(document)
                .map(documentMapper::toDomain);
    }

    @Override
    public Product saveToSearch(Product product) {
        ProductDocument doc = documentMapper.toDocument(product);
        ProductDocument saved = elasticsearchTemplate.save(doc);
        return documentMapper.toDomain(saved);
    }
}