package com.notificationservice.subsystem;

import com.notificationservice.application.port.in.SendOtpEmailUseCase;
import com.notificationservice.application.port.out.ProcessedEventPort;
import com.notificationservice.domain.exception.DuplicateEventException;
import com.notificationservice.domain.model.OtpEmailPayload;
import com.notificationservice.infrastructure.messaging.listener.OtpEmailEventListener;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Subsystem testleri: listener -> usecase -> adapter zinciri GERÇEK bean'ler ve
 * GERÇEK altyapı (Postgres + GreenMail SMTP) ile uçtan uca doğrulanır. Mock yok.
 */
@DisplayName("Notification Service - Subsystem Tests (Postgres + RabbitMQ + GreenMail, gerçek bean zinciri)")
@org.junit.jupiter.api.condition.EnabledIf("com.notificationservice.support.DockerAvailability#isDockerAvailable")
class OtpNotificationSubsystemTest extends AbstractNotificationSubsystemTest {

    @Autowired
    private OtpEmailEventListener listener;

    @Autowired
    private SendOtpEmailUseCase sendOtpEmailUseCase;

    @Autowired
    private ProcessedEventPort processedEventPort;

    @Test
    @DisplayName("S1: listener -> usecase -> SMTP - Event işlenince mail teslim edilir ve processed_event'e SENT yazılır")
    void handleOtpEvent_WhenFirstDelivery_ShouldSendMailAndPersistSentRecord() throws Exception {
        listener.handleOtpEvent(new OtpEmailPayload("s1@shopbridge.com", "111222"), "msg-s1");

        assertThat(greenMail().waitForIncomingEmail(10000, 1)).isTrue();

        MimeMessage[] received = greenMail().getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("s1@shopbridge.com");
        assertThat(received[0].getSubject()).isEqualTo("ShopBridge - Doğrulama Kodunuz");

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT event_type, status FROM processed_event WHERE idempotency_key = ?",
                "msg-s1-s1@shopbridge.com");
        assertThat(row.get("event_type")).isEqualTo("auth.event.otp");
        assertThat(row.get("status")).isEqualTo("SENT");
    }

    @Test
    @DisplayName("S2: IDEMPOTENCY - Aynı event iki kez teslim edilirse mail SADECE BİR KEZ gider")
    void handleOtpEvent_WhenSameEventDeliveredTwice_ShouldSendMailExactlyOnce() {
        OtpEmailPayload payload = new OtpEmailPayload("s2@shopbridge.com", "333444");

        listener.handleOtpEvent(payload, "msg-s2");
        assertThat(greenMail().waitForIncomingEmail(10000, 1)).isTrue();

        // İkinci teslimat: DuplicateEventException listener içinde yutulur (ACK).
        assertThatCode(() -> listener.handleOtpEvent(payload, "msg-s2")).doesNotThrowAnyException();

        assertThat(greenMail().getReceivedMessages())
                .as("aynı idempotency key ile ikinci mail gönderilmemeli")
                .hasSize(1);
        assertThat(processedEventCount("msg-s2-s2@shopbridge.com")).isEqualTo(1);
    }

    @Test
    @DisplayName("S3: IDEMPOTENCY (messageId'siz) - Fallback hash aynı payload için tekrar mail göndermez")
    void handleOtpEvent_WhenSamePayloadWithoutMessageId_ShouldStillDeduplicate() {
        OtpEmailPayload payload = new OtpEmailPayload("s3@shopbridge.com", "555666");

        listener.handleOtpEvent(payload, null);
        assertThat(greenMail().waitForIncomingEmail(10000, 1)).isTrue();

        listener.handleOtpEvent(new OtpEmailPayload("s3@shopbridge.com", "555666"), null);

        assertThat(greenMail().getReceivedMessages()).hasSize(1);
    }

    @Test
    @DisplayName("S4: usecase - Doğrudan çağrıda ikinci kez DuplicateEventException fırlar (listener'ın yutmadığı katman)")
    void sendOtpEmail_WhenCalledTwiceDirectly_ShouldThrowDuplicateEventExceptionOnSecondCall() {
        OtpEmailPayload payload = new OtpEmailPayload("s4@shopbridge.com", "777888");

        sendOtpEmailUseCase.sendOtpEmail("key-s4", payload);
        assertThat(greenMail().waitForIncomingEmail(10000, 1)).isTrue();

        assertThatThrownBy(() -> sendOtpEmailUseCase.sendOtpEmail("key-s4", payload))
                .isInstanceOf(DuplicateEventException.class);

        assertThat(greenMail().getReceivedMessages()).hasSize(1);
        assertThat(processedEventCount("key-s4")).isEqualTo(1);
    }

    @Test
    @DisplayName("S5: Farklı kullanıcılar - Ayrı idempotency key'ler birbirini bloklamaz, her ikisine de mail gider")
    void handleOtpEvent_WhenDifferentRecipients_ShouldSendSeparateMails() {
        listener.handleOtpEvent(new OtpEmailPayload("s5-a@shopbridge.com", "101010"), "msg-s5-a");
        listener.handleOtpEvent(new OtpEmailPayload("s5-b@shopbridge.com", "202020"), "msg-s5-b");

        assertThat(greenMail().waitForIncomingEmail(10000, 2)).isTrue();
        assertThat(greenMail().getReceivedMessages()).hasSize(2);
        assertThat(processedEventCount("msg-s5-a-s5-a@shopbridge.com")).isEqualTo(1);
        assertThat(processedEventCount("msg-s5-b-s5-b@shopbridge.com")).isEqualTo(1);
    }

    @Test
    @DisplayName("S6: Hata yolu - Alıcı adresi bozuksa mail gitmez, processed_event YAZILMAZ ve hata listener'dan yayılır")
    void handleOtpEvent_WhenRecipientMalformed_ShouldNotPersistProcessedEventAndPropagate() {
        assertThatThrownBy(() -> listener.handleOtpEvent(new OtpEmailPayload("bozuk adres @@", "123123"), "msg-s6"))
                .isInstanceOf(RuntimeException.class);

        assertThat(greenMail().getReceivedMessages()).isEmpty();
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM processed_event", Integer.class);
        assertThat(total).isZero();
    }

    @Test
    @DisplayName("S7: ProcessedEventPort bean'i gerçek JDBC adapter'dır ve schema.sql ile uyumlu çalışır")
    void processedEventPort_ShouldBeBackedByRealDatabaseSchema() {
        assertThat(processedEventPort.isEventProcessed("hic-olmayan-key")).isFalse();

        listener.handleOtpEvent(new OtpEmailPayload("s7@shopbridge.com", "909090"), "msg-s7");
        assertThat(greenMail().waitForIncomingEmail(10000, 1)).isTrue();

        assertThat(processedEventPort.isEventProcessed("msg-s7-s7@shopbridge.com")).isTrue();
    }

    @Test
    @DisplayName("S8: Mail içeriği - Gönderilen mailin gövdesinde doğru OTP kodu bulunur (HTML + düz metin)")
    void handleOtpEvent_WhenProcessed_ShouldRenderOtpCodeInBothBodyParts() throws Exception {
        listener.handleOtpEvent(new OtpEmailPayload("s8@shopbridge.com", "424242"), "msg-s8");

        assertThat(greenMail().waitForIncomingEmail(10000, 1)).isTrue();

        String raw = com.icegreen.greenmail.util.GreenMailUtil.getWholeMessage(greenMail().getReceivedMessages()[0]);
        assertThat(raw).contains("424242");
        assertThat(raw).contains("multipart/alternative");
    }
}
