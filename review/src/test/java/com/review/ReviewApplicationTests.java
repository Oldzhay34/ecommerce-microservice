package com.review;

import com.review.subsystem.AbstractReviewSubsystemTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Çıplak {@code @SpringBootTest} olarak bırakılamaz: review-service Postgres,
 * RabbitMQ ve Elasticsearch olmadan context'i ayağa kaldıramaz - ne yerelde
 * ne de CI'da geçerdi. order servisindeki kalıp izlenerek Testcontainers
 * tabanı devralınır.
 *
 * NOT: Docker gerektirir; Docker'sız makinede çalıştırılamaz.
 */
@DisplayName("Review Service - Spring context yüklenebilirlik testi")
class ReviewApplicationTests extends AbstractReviewSubsystemTest {

    @Test
    @DisplayName("S0: Uygulama context'i gerçek altyapıyla birlikte sorunsuz yüklenir")
    void contextLoads_WithRealInfrastructure_ShouldStartSuccessfully() {
    }
}
