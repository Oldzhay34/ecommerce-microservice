package com.promptengineering;

import com.promptengineering.auth.infrastructure.security.provider.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory; // <-- BURASI DEĞİŞTİ
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Uygulama context'inin gercekten ayaga kalktigini dogrulayan smoke test.
 */
@SpringBootTest
@ActiveProfiles("test")
class PromptEngineeringApplicationTests {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    // ÇÖZÜM UYGULANDI: Spring'in hem senkron hem reaktif Redis konfigürasyonlarını
    // tatmin etmek için iki arayüzü de barındıran LettuceConnectionFactory mock'landı.
    @MockitoBean
    private LettuceConnectionFactory redisConnectionFactory;

    // JWT Security Context'inin ayağa kalkarken patlamaması için eklendi
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void contextLoads() {
    }
}