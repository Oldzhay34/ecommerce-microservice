package com.order.publisher;

import com.order.infrastructure.messaging.publisher.OutboxEventPublisher;
import com.order.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.order.infrastructure.persistence.repository.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;

    @ParameterizedTest(name = "U23-{index}: {0} -> routing key {1}")
    @DisplayName("U23: publishOutboxEvents - eventType değerine göre doğru routing key kullanılır")
    @CsvSource({
            "OrderCreatedEvent,order.created",
            "OrderApprovedEvent,order.approved",
            "OrderCancelledEvent,order.cancelled",
            "OrderShippedEvent,order.shipped",
            "SomeUnknownEvent,order.generic"
    })
    void publishOutboxEvents_ShouldMapEventTypeToCorrectRoutingKey(String eventType, String expectedRoutingKey) {
        OutboxEventJpaEntity event = new OutboxEventJpaEntity();
        event.setEventType(eventType);
        event.setPayload("{\"id\":\"order-123\"}");

        when(outboxEventRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));

        outboxEventPublisher.publishOutboxEvents();

        verify(rabbitTemplate).convertAndSend(eq("order.exchange"), eq(expectedRoutingKey), anyString());
    }

    @Test
    @DisplayName("U24: publishOutboxEvents - Yayınlanan event processed=true olarak işaretlenip kaydedilir")
    void publishOutboxEvents_ShouldMarkEventAsProcessed() {
        OutboxEventJpaEntity event = new OutboxEventJpaEntity();
        event.setEventType("OrderCreatedEvent");
        event.setPayload("{\"id\":\"order-123\"}");
        event.setProcessed(false);

        when(outboxEventRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));

        outboxEventPublisher.publishOutboxEvents();

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().isProcessed()).isTrue();
    }

    @Test
    @DisplayName("U25: publishOutboxEvents - Bekleyen event yoksa hiçbir mesaj gönderilmez")
    void publishOutboxEvents_WhenNoUnprocessedEvents_ShouldNotSendAnything() {
        when(outboxEventRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of());

        outboxEventPublisher.publishOutboxEvents();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
        verify(outboxEventRepository, never()).save(any());
    }
}