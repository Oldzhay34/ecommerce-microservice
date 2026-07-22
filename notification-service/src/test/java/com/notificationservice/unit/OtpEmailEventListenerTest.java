package com.notificationservice.unit;

import com.notificationservice.application.port.in.SendOtpEmailUseCase;
import com.notificationservice.domain.exception.DuplicateEventException;
import com.notificationservice.domain.exception.EmailSendException;
import com.notificationservice.domain.model.OtpEmailPayload;
import com.notificationservice.infrastructure.messaging.listener.OtpEmailEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Notification Service - Unit: OtpEmailEventListener (idempotency key üretimi + hata davranışı)")
@ExtendWith(MockitoExtension.class)
class OtpEmailEventListenerTest {

    @Mock
    private SendOtpEmailUseCase sendOtpEmailUseCase;

    @InjectMocks
    private OtpEmailEventListener listener;

    private OtpEmailPayload payload;

    @BeforeEach
    void setUp() {
        payload = new OtpEmailPayload("customer@shopbridge.com", "483920");
    }

    private static String expectedFallbackHash(String email, String otpCode) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest((email + ":" + otpCode).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    @Test
    @DisplayName("U6: handleOtpEvent - messageId varsa idempotency key 'messageId-email' formatında üretilir")
    void handleOtpEvent_WhenMessageIdPresent_ShouldBuildKeyFromMessageIdAndEmail() {
        listener.handleOtpEvent(payload, "msg-123");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendOtpEmailUseCase).sendOtpEmail(keyCaptor.capture(), eq(payload));
        assertThat(keyCaptor.getValue()).isEqualTo("msg-123-customer@shopbridge.com");
    }

    @Test
    @DisplayName("U7: handleOtpEvent - messageId null ise email+otp üzerinden SHA-256 fallback hash üretilir")
    void handleOtpEvent_WhenMessageIdIsNull_ShouldUseSha256FallbackHash() throws Exception {
        listener.handleOtpEvent(payload, null);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendOtpEmailUseCase).sendOtpEmail(keyCaptor.capture(), eq(payload));
        assertThat(keyCaptor.getValue())
                .isEqualTo(expectedFallbackHash("customer@shopbridge.com", "483920"));
    }

    @Test
    @DisplayName("U8: handleOtpEvent - messageId boşluktan ibaretse yine fallback hash kullanılır")
    void handleOtpEvent_WhenMessageIdIsBlank_ShouldUseSha256FallbackHash() throws Exception {
        listener.handleOtpEvent(payload, "   ");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendOtpEmailUseCase).sendOtpEmail(keyCaptor.capture(), eq(payload));
        assertThat(keyCaptor.getValue())
                .isEqualTo(expectedFallbackHash("customer@shopbridge.com", "483920"));
    }

    @Test
    @DisplayName("U9: handleOtpEvent - Fallback hash deterministiktir: aynı payload iki kez gelirse aynı key üretilir")
    void handleOtpEvent_WhenSamePayloadDeliveredTwice_ShouldProduceIdenticalFallbackKey() {
        listener.handleOtpEvent(new OtpEmailPayload("a@b.com", "111111"), null);
        listener.handleOtpEvent(new OtpEmailPayload("a@b.com", "111111"), null);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendOtpEmailUseCase, times(2)).sendOtpEmail(keyCaptor.capture(), any());
        assertThat(keyCaptor.getAllValues().get(0)).isEqualTo(keyCaptor.getAllValues().get(1));
    }

    @Test
    @DisplayName("U10: handleOtpEvent - DuplicateEventException yutulur (mesaj ACK'lenir, requeue/DLQ tetiklenmez)")
    void handleOtpEvent_WhenDuplicateEventException_ShouldSwallowAndAcknowledge() {
        doThrow(new DuplicateEventException("already processed"))
                .when(sendOtpEmailUseCase).sendOtpEmail(any(), any());

        assertThatCode(() -> listener.handleOtpEvent(payload, "msg-dup"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("U11: handleOtpEvent - EmailSendException yutulmaz, yayılır (retry/DLQ mekanizması tetiklenir)")
    void handleOtpEvent_WhenEmailSendException_ShouldPropagateForRetryAndDlq() {
        doThrow(new EmailSendException("SMTP down", new RuntimeException()))
                .when(sendOtpEmailUseCase).sendOtpEmail(any(), any());

        assertThatThrownBy(() -> listener.handleOtpEvent(payload, "msg-fail"))
                .isInstanceOf(EmailSendException.class);
    }

    @Test
    @DisplayName("U12: handleOtpEvent - Beklenmeyen RuntimeException de yutulmaz, listener dışına yayılır")
    void handleOtpEvent_WhenUnexpectedRuntimeException_ShouldPropagate() {
        doThrow(new IllegalStateException("beklenmeyen"))
                .when(sendOtpEmailUseCase).sendOtpEmail(any(), any());

        assertThatThrownBy(() -> listener.handleOtpEvent(payload, "msg-boom"))
                .isInstanceOf(IllegalStateException.class);
    }
}
