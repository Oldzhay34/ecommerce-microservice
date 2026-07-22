package com.review.unit.messaging;

import com.review.infrastructure.messaging.publisher.OutboxEventPublisher;
import com.review.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.review.infrastructure.persistence.repository.OutboxEventRepository;
import com.review.unit.support.ReviewTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Outbox yayınlama mantığının whitebox testi. review servisinde routing key
 * seçimi order servisindeki gibi bir switch/case DEĞİL, event.getType()
 * üzerinden doğrudan pass-through'dur; bu testler o sözleşmeyi her event tipi
 * için (bilinen + bilinmeyen) sabitler.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Review Service - Unit: OutboxEventPublisher")
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxEventPublisher publisher;

    @ParameterizedTest(name = "U35-{index}: {0} -> review.exchange/{1}")
    @CsvSource({
            "review.created,review.created",
            "review.moderated,review.moderated",
            "review.replied,review.replied",
            "review.deleted,review.deleted",
            "some.unknown.type,some.unknown.type"
    })
    @DisplayName("U35: publishEvents - Event tipi routing key olarak review.exchange'e geçirilir (her case)")
    void publishEvents_ShouldUseEventTypeAsRoutingKey(String type, String expectedRoutingKey) {
        OutboxEventJpaEntity event = ReviewTestFixtures.outboxEvent("r-1", type, "{\"productId\":\"prod-1\"}");
        when(outboxEventRepository.findAll()).thenReturn(List.of(event));

        publisher.publishEvents();

        verify(rabbitTemplate).convertAndSend(eq("review.exchange"), eq(expectedRoutingKey),
                eq("{\"productId\":\"prod-1\"}"));
    }

    @Test
    @DisplayName("U36: publishEvents - Yayınlanan event outbox tablosundan silinir (at-least-once temizliği)")
    void publishEvents_WhenEventPublished_ShouldDeleteItFromOutbox() {
        OutboxEventJpaEntity event = ReviewTestFixtures.outboxEvent("r-1", "review.created", "{}");
        when(outboxEventRepository.findAll()).thenReturn(List.of(event));

        publisher.publishEvents();

        verify(outboxEventRepository).delete(event);
    }

    @Test
    @DisplayName("U37: publishEvents - Silme işlemi yayınlamadan SONRA yapılır (mesaj kaybı olmaz)")
    void publishEvents_ShouldPublishBeforeDeleting() {
        OutboxEventJpaEntity event = ReviewTestFixtures.outboxEvent("r-1", "review.created", "{}");
        when(outboxEventRepository.findAll()).thenReturn(List.of(event));

        publisher.publishEvents();

        InOrder order = inOrder(rabbitTemplate, outboxEventRepository);
        order.verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
        order.verify(outboxEventRepository).delete(event);
    }

    @Test
    @DisplayName("U38: publishEvents - Bekleyen event yoksa hiçbir mesaj gönderilmez")
    void publishEvents_WhenOutboxIsEmpty_ShouldNotSendAnything() {
        when(outboxEventRepository.findAll()).thenReturn(List.of());

        publisher.publishEvents();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
        verify(outboxEventRepository, never()).delete(any(OutboxEventJpaEntity.class));
    }

    @Test
    @DisplayName("U39: publishEvents - Birden fazla event varsa hepsi tek turda yayınlanır")
    void publishEvents_WhenMultipleEvents_ShouldPublishAllOfThem() {
        when(outboxEventRepository.findAll()).thenReturn(List.of(
                ReviewTestFixtures.outboxEvent("r-1", "review.created", "{\"a\":1}"),
                ReviewTestFixtures.outboxEvent("r-2", "review.moderated", "{\"b\":2}")));

        publisher.publishEvents();

        verify(rabbitTemplate).convertAndSend("review.exchange", "review.created", "{\"a\":1}");
        verify(rabbitTemplate).convertAndSend("review.exchange", "review.moderated", "{\"b\":2}");
        verify(outboxEventRepository, org.mockito.Mockito.times(2)).delete(any(OutboxEventJpaEntity.class));
    }

    @Test
    @DisplayName("U40: publishEvents - Yayınlama hata verirse event SİLİNMEZ, hata yukarı taşınır (rollback)")
    void publishEvents_WhenBrokerFails_ShouldNotDeleteEventAndPropagate() {
        OutboxEventJpaEntity event = ReviewTestFixtures.outboxEvent("r-1", "review.created", "{}");
        when(outboxEventRepository.findAll()).thenReturn(List.of(event));
        doThrow(new org.springframework.amqp.AmqpException("broker down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> publisher.publishEvents())
                .isInstanceOf(org.springframework.amqp.AmqpException.class);

        verify(outboxEventRepository, never()).delete(any(OutboxEventJpaEntity.class));
    }
}
