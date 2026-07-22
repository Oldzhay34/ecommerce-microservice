package com.notificationservice.unit;

import com.notificationservice.application.usecase.SendOtpEmailUseCaseImpl;
import com.notificationservice.application.port.out.EmailSenderPort;
import com.notificationservice.application.port.out.ProcessedEventPort;
import com.notificationservice.domain.exception.DuplicateEventException;
import com.notificationservice.domain.exception.EmailSendException;
import com.notificationservice.domain.model.OtpEmailPayload;
import com.notificationservice.domain.model.ProcessedEvent;
import com.notificationservice.domain.model.ProcessedEventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Notification Service - Unit: SendOtpEmailUseCaseImpl (saf Mockito)")
@ExtendWith(MockitoExtension.class)
class SendOtpEmailUseCaseImplTest {

    @Mock
    private ProcessedEventPort processedEventPort;

    @Mock
    private EmailSenderPort emailSenderPort;

    @InjectMocks
    private SendOtpEmailUseCaseImpl useCase;

    private OtpEmailPayload payload;

    @BeforeEach
    void setUp() {
        payload = new OtpEmailPayload("customer@shopbridge.com", "483920");
    }

    @Test
    @DisplayName("U1: sendOtpEmail - Event daha önce işlenmemişse maili gönderir ve processed_event kaydı yazar")
    void sendOtpEmail_WhenEventNotProcessed_ShouldSendMailAndPersistProcessedEvent() {
        when(processedEventPort.isEventProcessed("key-1")).thenReturn(false);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        useCase.sendOtpEmail("key-1", payload);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        verify(emailSenderPort).sendOtpEmail(payload);

        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventPort).save(captor.capture());

        ProcessedEvent saved = captor.getValue();
        assertThat(saved.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(saved.getEventType()).isEqualTo("auth.event.otp");
        assertThat(saved.getStatus()).isEqualTo(ProcessedEventStatus.SENT);
        assertThat(saved.getCreatedAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("U2: sendOtpEmail - Aynı idempotency key ikinci kez gelirse DuplicateEventException fırlatır ve mail GÖNDERİLMEZ")
    void sendOtpEmail_WhenEventAlreadyProcessed_ShouldThrowDuplicateAndNeverSendMail() {
        when(processedEventPort.isEventProcessed("key-dup")).thenReturn(true);

        assertThatThrownBy(() -> useCase.sendOtpEmail("key-dup", payload))
                .isInstanceOf(DuplicateEventException.class)
                .hasMessageContaining("key-dup");

        verify(emailSenderPort, never()).sendOtpEmail(any());
        verify(processedEventPort, never()).save(any());
    }

    @Test
    @DisplayName("U3: sendOtpEmail - Mail gönderimi patlarsa EmailSendException yayılır ve processed_event YAZILMAZ (retry mümkün kalır)")
    void sendOtpEmail_WhenEmailSenderFails_ShouldPropagateAndNotMarkAsProcessed() {
        when(processedEventPort.isEventProcessed("key-fail")).thenReturn(false);
        doThrow(new EmailSendException("SMTP down", new RuntimeException()))
                .when(emailSenderPort).sendOtpEmail(payload);

        assertThatThrownBy(() -> useCase.sendOtpEmail("key-fail", payload))
                .isInstanceOf(EmailSendException.class);

        verify(processedEventPort, never()).save(any());
    }

    @Test
    @DisplayName("U4: sendOtpEmail - Sıralama garantisi: önce idempotency kontrolü, sonra mail, en son kayıt")
    void sendOtpEmail_WhenEventNotProcessed_ShouldExecuteInCheckSendPersistOrder() {
        when(processedEventPort.isEventProcessed("key-order")).thenReturn(false);

        useCase.sendOtpEmail("key-order", payload);

        InOrder inOrder = inOrder(processedEventPort, emailSenderPort);
        inOrder.verify(processedEventPort).isEventProcessed("key-order");
        inOrder.verify(emailSenderPort).sendOtpEmail(payload);
        inOrder.verify(processedEventPort).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("U5: sendOtpEmail - Aynı payload farklı key ile gelirse ayrı event sayılır ve mail tekrar gönderilir")
    void sendOtpEmail_WhenSamePayloadWithDifferentKey_ShouldSendMailAgain() {
        when(processedEventPort.isEventProcessed("key-a")).thenReturn(false);
        when(processedEventPort.isEventProcessed("key-b")).thenReturn(false);

        useCase.sendOtpEmail("key-a", payload);
        useCase.sendOtpEmail("key-b", payload);

        verify(emailSenderPort, times(2)).sendOtpEmail(payload);
        verify(processedEventPort, times(2)).save(any(ProcessedEvent.class));
    }
}
