package com.review.infrastructure.search.adapter;

import com.review.application.port.out.ReviewQueryPort;
import com.review.domain.model.Review;
import com.review.infrastructure.search.document.ReviewDocument;
import com.review.infrastructure.search.mapper.ReviewDocumentMapper;
import com.review.infrastructure.search.repository.ReviewSearchRepository;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ReviewSearchAdapter implements ReviewQueryPort {

    private final ReviewSearchRepository repository;
    private final ReviewDocumentMapper mapper;

    public ReviewSearchAdapter(ReviewSearchRepository repository, ReviewDocumentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public void syncToElasticsearch(Review review) {
        repository.save(mapper.toDocument(review));
    }

    @Override
    public Optional<Review> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Review> findActiveByProductId(String productId) {
        return repository.findByProductIdAndStatus(productId, "ACTIVE")
                .stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Review> findByCustomerId(String customerId) {
        return repository.findByCustomerId(customerId)
                .stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Review> findAll() {
        List<Review> list = new ArrayList<>();
        repository.findAll().forEach(doc -> list.add(mapper.toDomain(doc)));
        return list;
    }

    @Override
    public double getAverageRatingForProduct(String productId) {
        List<ReviewDocument> reviews = repository.findByProductIdAndStatus(productId, "ACTIVE");
        if (reviews.isEmpty()) return 0.0;
        return reviews.stream().mapToInt(ReviewDocument::getRating).average().orElse(0.0);
    }
}