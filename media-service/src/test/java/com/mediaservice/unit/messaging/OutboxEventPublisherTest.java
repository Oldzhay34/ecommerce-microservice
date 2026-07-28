package com.mediaservice.unit.messaging;

import com.mediaservice.infrastructure.config.RabbitMqConfig;
import com.mediaservice.infrastructure.messaging.publisher.OutboxEventPublisher;
import com.mediaservice.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.mediaservice.infrastructure.persistence.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT - Transactional Outbox'in yayin ayagi. Repository + RabbitTemplate mock'lanir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - OutboxEventPublisher")
class OutboxEventPublisherTest {

    @Mock private OutboxEventRepository repository;
    @Mock private RabbitTemplate rabbitTemplate;

    private OutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxEventPublisher(repository, rabbitTemplate);
    }

    @Test
    @DisplayName("U1: publishPendingEvents - Bekleyen olay yoksa RabbitMQ'ya HIC gidilmez")
    void publishPendingEvents_WhenNoneUnprocessed_ShouldNotTouchBroker() {
        when(repository.lockUnprocessedBatch(100)).thenReturn(List.of());

        publisher.publishPendingEvents();

        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
        verify(repository, never()).markProcessed(anyList(), any());
    }

    @Test
    @DisplayName("U2: publishPendingEvents - Basarili yayindan sonra media.exchange'e basilir ve processed=true isaretlenir")
    void publishPendingEvents_WhenEventsPending_ShouldPublishAndMarkProcessed() {
        OutboxEventJpaEntity event = entity("media.uploaded");
        when(repository.lockUnprocessedBatch(100)).thenReturn(List.of(event));

        publisher.publishPendingEvents();

        verify(rabbitTemplate).send(eq(RabbitMqConfig.MEDIA_EXCHANGE), eq("media.uploaded"), any(Message.class));
        verify(repository).markProcessed(eq(List.of(event.getId())), any(Instant.class));
    }

    @Test
    @DisplayName("U3: publishPendingEvents - Kismi hata: basarisiz olan islenmis sayilmaz, basarili olan isaretlenir")
    void publishPendingEvents_WhenOnePublishFails_ShouldOnlyMarkSucceededOnes() {
        OutboxEventJpaEntity ok = entity("media.uploaded");
        OutboxEventJpaEntity failing = entity("media.deleted");
        when(repository.lockUnprocessedBatch(100)).thenReturn(List.of(ok, failing));
        // Basarili yol da ACIKCA stub'lanmali: MockitoExtension STRICT_STUBS ile calisir ve
        // ayni metodun stub'lanmamis argumanlarla cagrilmasi PotentialStubbingProblem
        // firlatir. Bu stub olmadan "ok" event'inin yayini da hata alir ve test, olcmek
        // istedigi kismi-hata senaryosu yerine "hicbiri yayinlanmadi" durumunu olcer.
        doNothing()
                .when(rabbitTemplate).send(eq(RabbitMqConfig.MEDIA_EXCHANGE), eq("media.uploaded"), any(Message.class));
        doThrow(new RuntimeException("broker unreachable"))
                .when(rabbitTemplate).send(eq(RabbitMqConfig.MEDIA_EXCHANGE), eq("media.deleted"), any(Message.class));

        publisher.publishPendingEvents();

        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).markProcessed(captor.capture(), any(Instant.class));
        assertThat(captor.getValue()).containsExactly(ok.getId());
    }

    private OutboxEventJpaEntity entity(String routingKey) {
        OutboxEventJpaEntity event = new OutboxEventJpaEntity();
        event.setId(UUID.randomUUID());
        event.setAggregateType("MediaAsset");
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("MediaUploadedEvent");
        event.setRoutingKey(routingKey);
        event.setPayload("{}");
        event.setProcessed(false);
        event.setCreatedAt(Instant.now());
        return event;
    }
}
