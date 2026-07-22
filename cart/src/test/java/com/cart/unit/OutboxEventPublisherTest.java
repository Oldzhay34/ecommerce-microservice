package com.cart.unit;

import com.cart.infrastructure.messaging.publisher.OutboxEventPublisher;
import com.cart.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.cart.infrastructure.persistence.repository.OutboxEventRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT.
 * Hedef: routing key türetiminin HER case'i (bilinen event tipleri, karışık
 * harf, bilinmeyen tip, null tip), processed işaretleme ve boş batch guard'ı.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - OutboxEventPublisher")
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxEventPublisher publisher;

    private OutboxEventJpaEntity event(String eventType) {
        OutboxEventJpaEntity event = new OutboxEventJpaEntity();
        event.setAggregateId("11111111-1111-1111-1111-111111111111");
        event.setEventType(eventType);
        event.setPayload("{\"userId\":\"11111111-1111-1111-1111-111111111111\"}");
        return event;
    }

    @ParameterizedTest(name = "U27-{index}: {0} -> {1}")
    @DisplayName("U27: publishEvents - eventType'a göre routing key türetiminin her case'i")
    @CsvSource({
            "CartUpdatedEvent,cart.cartupdatedevent",
            "CartClearedEvent,cart.cartclearedevent",
            "CARTUPDATEDEVENT,cart.cartupdatedevent",
            "someUnknownEvent,cart.someunknownevent"
    })
    void publishEvents_ShouldDeriveRoutingKeyFromEventType(String eventType, String expectedRoutingKey) {
        when(outboxEventRepository.findByProcessedFalse()).thenReturn(List.of(event(eventType)));

        publisher.publishEvents();

        verify(rabbitTemplate).convertAndSend(eq("cart.exchange"), eq(expectedRoutingKey), anyString());
    }

    @Test
    @DisplayName("U28: publishEvents - Yayınlanan event processed=true olarak işaretlenip kaydedilir")
    void publishEvents_ShouldMarkEventAsProcessed() {
        OutboxEventJpaEntity pending = event("CartUpdatedEvent");
        assertThat(pending.isProcessed()).isFalse();
        when(outboxEventRepository.findByProcessedFalse()).thenReturn(List.of(pending));

        publisher.publishEvents();

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().isProcessed()).isTrue();
    }

    @Test
    @DisplayName("U29: publishEvents - Bekleyen event yoksa hiçbir mesaj gönderilmez")
    void publishEvents_WhenNoPendingEvents_ShouldSendNothing() {
        when(outboxEventRepository.findByProcessedFalse()).thenReturn(List.of());

        publisher.publishEvents();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("U30: publishEvents - Birden fazla bekleyen event varsa her biri ayrı ayrı yayınlanır")
    void publishEvents_WhenMultiplePendingEvents_ShouldPublishEachOne() {
        when(outboxEventRepository.findByProcessedFalse())
                .thenReturn(List.of(event("CartUpdatedEvent"), event("CartClearedEvent")));

        publisher.publishEvents();

        verify(rabbitTemplate).convertAndSend(eq("cart.exchange"), eq("cart.cartupdatedevent"), anyString());
        verify(rabbitTemplate).convertAndSend(eq("cart.exchange"), eq("cart.cartclearedevent"), anyString());
        verify(outboxEventRepository, times(2)).save(any(OutboxEventJpaEntity.class));
    }

    @Test
    @DisplayName("U31: publishEvents - eventType null ise NullPointerException fırlatır (bilinen sınır davranışı)")
    void publishEvents_WhenEventTypeIsNull_ShouldThrowNullPointerException() {
        when(outboxEventRepository.findByProcessedFalse()).thenReturn(List.of(event(null)));

        assertThatThrownBy(() -> publisher.publishEvents())
                .isInstanceOf(NullPointerException.class);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }
}
