package com.promptengineering;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Uygulama context'inin gercekten ayaga kalktigini dogrulayan smoke test.
 *
 * Bu projede context'i patlatabilecek dis bagimliliklar var:
 *   - RabbitMQ (spring-boot-starter-amqp)
 *   - Redis (spring-boot-starter-data-redis)
 *   - Spring AI Anthropic (spring-ai-starter-model-anthropic)
 *
 * COZUM YAKLASIMI: String tabanli "spring.autoconfigure.exclude" KULLANMIYORUZ
 * (Spring Boot 4 modularizasyonunda o string'ler classpath'te bulunamayip "Cannot resolve"
 * hatasi verebiliyor). Bunun yerine dis sistemlerin client bean'lerini @MockitoBean ile
 * saglıyoruz. Mock bean context'te oldugu icin:
 *   - RabbitTemplate mock -> auto-config gercek broker'a baglanmaya calismaz
 *   - StringRedisTemplate + RedisConnectionFactory mock -> Redis baglantisi acilmaz
 *
 * @MockitoBean (Spring Boot 4.x; eski @MockBean KALDIRILDI):
 *   org.springframework.test.context.bean.override.mockito.MockitoBean
 *
 * @ActiveProfiles("test") -> src/test/resources/application-test.yml devreye girer
 * (H2, scheduled kapali, dummy broker/redis baglanti bilgileri).
 *
 * NOT (Spring AI): Eger context Anthropic API key isteyip patlarsa, application-test.yml'e
 * spring.ai.anthropic.api-key=test-dummy-key eklenebilir ya da ilgili AI ChatModel bean'i
 * de @MockitoBean ile saglanabilir. Hata mesajina gore net cozum uygulanmali.
 */
@SpringBootTest
@ActiveProfiles("test")
class PromptEngineeringApplicationTests {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void contextLoads() {
    }
}