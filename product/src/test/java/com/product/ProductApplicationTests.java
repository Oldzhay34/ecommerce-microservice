package com.product;

import com.product.infrastructure.security.filter.JwtResourceFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Smoke Test - Context Yükleme")
class ProductApplicationTests {

    // Gerçek RabbitMQ'ya bağlanmasını engeller
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    // Gerçek Redis'e bağlanmasını engeller (Reactive+Sync destekli factory)
    @MockitoBean
    private LettuceConnectionFactory redisConnectionFactory;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    // Gerçek Elasticsearch'e bağlanmasını engeller
    @MockitoBean
    private ElasticsearchTemplate elasticsearchTemplate;

    // Security context'in patlamasını engeller
    @MockitoBean
    private JwtResourceFilter jwtResourceFilter;

    @Test
    @DisplayName("S1: Spring Context Başarıyla Yüklenmeli")
    void contextLoads() {
        // Eğer bu test yeşil geçiyorsa, tüm bean'ler başarıyla resolve edilmiş demektir.
    }
}