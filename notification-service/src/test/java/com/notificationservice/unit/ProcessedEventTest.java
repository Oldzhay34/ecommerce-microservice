package com.notificationservice.unit;

import com.notificationservice.domain.model.ProcessedEvent;
import com.notificationservice.domain.model.ProcessedEventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notification Service - Unit: ProcessedEvent / ProcessedEventStatus")
class ProcessedEventTest {

    @Test
    @DisplayName("U21: ProcessedEventStatus - Yalnızca SENT ve FAILED durumları tanımlıdır (DB'ye yazılan ad karşılıkları sabittir)")
    void processedEventStatus_ShouldContainOnlySentAndFailedWithStableNames() {
        assertThat(ProcessedEventStatus.values())
                .containsExactly(ProcessedEventStatus.SENT, ProcessedEventStatus.FAILED);

        assertThat(ProcessedEventStatus.SENT.name()).isEqualTo("SENT");
        assertThat(ProcessedEventStatus.FAILED.name()).isEqualTo("FAILED");
        assertThat(ProcessedEventStatus.valueOf("SENT")).isEqualTo(ProcessedEventStatus.SENT);
    }

    @Test
    @DisplayName("U22: ProcessedEvent - Constructor tüm alanları doldurur, status SENT'ten FAILED'a geçirilebilir")
    void processedEvent_WhenStatusUpdated_ShouldTransitionFromSentToFailed() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 21, 10, 30, 0);
        ProcessedEvent event = new ProcessedEvent("key-1", "auth.event.otp", ProcessedEventStatus.SENT, now);

        assertThat(event.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(event.getEventType()).isEqualTo("auth.event.otp");
        assertThat(event.getStatus()).isEqualTo(ProcessedEventStatus.SENT);
        assertThat(event.getCreatedAt()).isEqualTo(now);

        event.setStatus(ProcessedEventStatus.FAILED);
        assertThat(event.getStatus()).isEqualTo(ProcessedEventStatus.FAILED);
    }

    @Test
    @DisplayName("U23: ProcessedEvent - Boş constructor ile oluşturulup setter'larla doldurulabilir (JDBC mapping için)")
    void processedEvent_WhenBuiltWithSetters_ShouldExposeAllFields() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        ProcessedEvent event = new ProcessedEvent();
        event.setIdempotencyKey("key-2");
        event.setEventType("auth.event.otp");
        event.setStatus(ProcessedEventStatus.FAILED);
        event.setCreatedAt(now);

        assertThat(event.getIdempotencyKey()).isEqualTo("key-2");
        assertThat(event.getEventType()).isEqualTo("auth.event.otp");
        assertThat(event.getStatus()).isEqualTo(ProcessedEventStatus.FAILED);
        assertThat(event.getCreatedAt()).isEqualTo(now);
    }
}
