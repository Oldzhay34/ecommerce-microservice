package com.product.infrastructure.persistence.adapter;

import com.product.application.port.out.ProductCommandPort;
import com.product.domain.model.Product;
import com.product.domain.model.Review;
import com.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.product.infrastructure.persistence.entity.ReviewJpaEntity;
import com.product.infrastructure.persistence.mapper.ProductEntityMapper;
import com.product.infrastructure.persistence.repository.ProductRepository;
import com.product.infrastructure.persistence.repository.ReviewRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProductPersistenceAdapter implements ProductCommandPort {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final ProductEntityMapper productMapper;
    private final JdbcTemplate jdbcTemplate;

    public ProductPersistenceAdapter(ProductRepository productRepository,
                                     ReviewRepository reviewRepository,
                                     ProductEntityMapper productMapper,
                                     JdbcTemplate jdbcTemplate) {
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.productMapper = productMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Product saveProduct(Product product) {
        ProductJpaEntity entity = productMapper.toEntity(product);
        return productMapper.toDomain(productRepository.save(entity));
    }

    @Override
    public Optional<Product> findProductById(UUID id) {
        return productRepository.findById(id).map(productMapper::toDomain);
    }

    @Override
    public Review saveReview(Review review) {
        ReviewJpaEntity entity = new ReviewJpaEntity(
                review.getId(), review.getProductId(), review.getCustomerId(),
                review.getComment(), review.getRating()
        );
        ReviewJpaEntity saved = reviewRepository.save(entity);
        return new Review(saved.getId(), saved.getProductId(), saved.getCustomerId(), saved.getComment(), saved.getRating());
    }

    @Override
    public void saveOutboxEvent(String aggregateType, String aggregateId, String eventType, String payload) {
        // aggregateType/aggregateId parametreleri port sözleşmesinde var ancak outbox_event
        // tablosunda karşılıkları yok ve OutboxEventPublisher routing key olarak yalnızca
        // event_type kullanıyor — bu yüzden persist edilmiyorlar.
        String sql = "INSERT INTO outbox_event (id, event_type, payload, created_at) " +
                "VALUES (?, ?, ?::jsonb, ?)";
        jdbcTemplate.update(sql,
                UUID.randomUUID(),
                eventType,
                payload,
                Timestamp.valueOf(LocalDateTime.now()));
    }
}