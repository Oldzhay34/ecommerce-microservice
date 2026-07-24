package com.mediaservice.subsystem;

import com.mediaservice.application.port.in.MediaCommandUseCase;
import com.mediaservice.infrastructure.config.RabbitMqConfig;
import com.mediaservice.infrastructure.persistence.repository.MediaAssetRepository;
import com.mediaservice.infrastructure.persistence.repository.OutboxEventRepository;
import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Katman: SUBSYSTEM - Transactional Outbox'in yayin ayagi gercek RabbitMQ uzerinde
 * dogrulanir. {@code @EnableScheduling} + {@code @Scheduled(fixedDelay = 2000)} sabit
 * kodlu oldugu icin (dis property'ye baglanmamis) devre disi BIRAKILAMAZ; testler
 * dogal periyodu {@code await()} ile bekler (payment'taki gibi elle tetikleme yerine).
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b>
 */
@EnabledIf("com.mediaservice.support.DockerAvailability#isDockerAvailable")
@DisplayName("SUBSYSTEM - Outbox yayinlayici (Postgres + RabbitMQ)")
class OutboxPublisherSubsystemTest extends AbstractMediaSubsystemTest {

    @Autowired private MediaCommandUseCase commandUseCase;
    @Autowired private MediaAssetRepository mediaAssetRepository;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private RabbitTemplate rabbitTemplate;

    private UUID productId;
    private UUID storeId;

    @BeforeEach
    void resetState() {
        outboxEventRepository.deleteAll();
        mediaAssetRepository.deleteAll();
        productId = UUID.randomUUID();
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("S1: Yayinlanan MediaUploadedEvent gercekten media.exchange'e duser ve outbox satiri processed=true olur")
    void publishedEvent_ShouldReachMediaExchangeAndBeMarkedProcessed() {
        String queueName = rabbitTemplate.execute(channel -> channel.queueDeclare().getQueue());
        rabbitTemplate.execute(channel -> {
            channel.queueBind(queueName, RabbitMqConfig.MEDIA_EXCHANGE, RabbitMqConfig.RK_MEDIA_UPLOADED);
            return null;
        });

        commandUseCase.uploadProductImage(productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            org.springframework.amqp.core.Message message = rabbitTemplate.receive(queueName, 500);
            assertThat(message).as("media.exchange/media.uploaded uzerine dusen mesaj").isNotNull();
            assertThat(new String(message.getBody())).contains(productId.toString());
        });

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(outboxEventRepository.findAll())
                        .as("basariyla yayinlanan satir processed=true olmali")
                        .allMatch(e -> e.isProcessed()));
    }

    @Test
    @DisplayName("S2: media.deleted routing key'i ile yayinlanan olay dogru kuyruga duser")
    void deleteEvent_ShouldPublishWithMediaDeletedRoutingKey() {
        var asset = commandUseCase.uploadProductImage(
                productId, storeId, false, "image/png", MediaTestFixtures.validPngBytes());

        String queueName = rabbitTemplate.execute(channel -> channel.queueDeclare().getQueue());
        rabbitTemplate.execute(channel -> {
            channel.queueBind(queueName, RabbitMqConfig.MEDIA_EXCHANGE, RabbitMqConfig.RK_MEDIA_DELETED);
            return null;
        });

        commandUseCase.deleteProductImage(asset.getId(), storeId, false);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(rabbitTemplate.receive(queueName, 500))
                        .as("media.deleted routing key'ine baglanan kuyrukta mesaj bulunmali")
                        .isNotNull());
    }
}
