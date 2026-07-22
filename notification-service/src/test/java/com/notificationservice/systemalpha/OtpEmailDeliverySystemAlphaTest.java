package com.notificationservice.systemalpha;

import com.icegreen.greenmail.util.GreenMailUtil;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * System-Alpha: uçtan uca black-box akış.
 * Giriş = RabbitMQ'ya publish edilen gerçek OTP event mesajı.
 * Çıkış = SMTP posta kutusuna düşen gerçek mail.
 */
@DisplayName("Notification Service - System Alpha (black-box: kuyruk -> mail kutusu)")
@org.junit.jupiter.api.condition.EnabledIf("com.notificationservice.support.DockerAvailability#isDockerAvailable")
class OtpEmailDeliverySystemAlphaTest extends AbstractNotificationSystemAlphaTest {

    @Test
    @DisplayName("A1: OTP event'i kuyruğa publish edilince alıcıya doğru konu ve OTP kodu ile mail düşer")
    void publishOtpEvent_WhenValidMessage_ShouldDeliverOtpMailToRecipient() throws Exception {
        publishOtpEvent("{\"email\":\"alpha1@shopbridge.com\",\"otpCode\":\"654321\"}", "alpha-msg-1");

        assertThat(mailbox().waitForIncomingEmail(20000, 1)).isTrue();

        MimeMessage[] received = mailbox().getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("alpha1@shopbridge.com");
        assertThat(received[0].getSubject()).isEqualTo("ShopBridge - Doğrulama Kodunuz");
        assertThat(GreenMailUtil.getWholeMessage(received[0])).contains("654321");
    }

    @Test
    @DisplayName("A2: IDEMPOTENCY - Aynı messageId ile aynı event iki kez publish edilirse mail kutusuna TEK mail düşer")
    void publishOtpEvent_WhenSameMessageIdPublishedTwice_ShouldDeliverExactlyOneMail() {
        String json = "{\"email\":\"alpha2@shopbridge.com\",\"otpCode\":\"111000\"}";

        publishOtpEvent(json, "alpha-msg-2");
        assertThat(mailbox().waitForIncomingEmail(20000, 1)).isTrue();

        publishOtpEvent(json, "alpha-msg-2");

        // İkinci mesaj işlenmiş olmasına rağmen yeni mail üretmemeli.
        await().during(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(mailbox().getReceivedMessages()).hasSize(1));
    }

    @Test
    @DisplayName("A3: IDEMPOTENCY (messageId'siz publisher) - Aynı payload iki kez gelse de tek mail gönderilir")
    void publishOtpEvent_WhenPublisherOmitsMessageId_ShouldStillDeliverExactlyOneMail() {
        String json = "{\"email\":\"alpha3@shopbridge.com\",\"otpCode\":\"222000\"}";

        publishOtpEvent(json, null);
        assertThat(mailbox().waitForIncomingEmail(20000, 1)).isTrue();

        publishOtpEvent(json, null);

        await().during(Duration.ofSeconds(3))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(mailbox().getReceivedMessages()).hasSize(1));
    }

    @Test
    @DisplayName("A4: Farklı alıcılar - İki ayrı OTP event'i iki ayrı maile dönüşür")
    void publishOtpEvent_WhenDifferentRecipients_ShouldDeliverTwoMails() {
        publishOtpEvent("{\"email\":\"alpha4-a@shopbridge.com\",\"otpCode\":\"333000\"}", "alpha-msg-4a");
        publishOtpEvent("{\"email\":\"alpha4-b@shopbridge.com\",\"otpCode\":\"444000\"}", "alpha-msg-4b");

        assertThat(mailbox().waitForIncomingEmail(20000, 2)).isTrue();
        assertThat(mailbox().getReceivedMessages()).hasSize(2);
    }

    @Test
    @DisplayName("A5: Şema evrimi - Publisher fazladan alan gönderse bile mail teslim edilir (DLQ'ya düşmez)")
    void publishOtpEvent_WhenMessageHasExtraFields_ShouldStillDeliverMail() {
        publishOtpEvent(
                "{\"email\":\"alpha5@shopbridge.com\",\"otpCode\":\"555000\",\"channel\":\"EMAIL\",\"issuedAt\":\"2026-07-21T10:15:30\"}",
                "alpha-msg-5");

        assertThat(mailbox().waitForIncomingEmail(20000, 1)).isTrue();
        assertThat(mailbox().getReceivedMessages()).hasSize(1);
    }

    @Test
    @DisplayName("A6: Bozuk mesaj - Deserialize edilemeyen gövde DLQ'ya yönlendirilir ve hiç mail gönderilmez")
    void publishOtpEvent_WhenBodyIsNotValidJson_ShouldRouteToDlqAndSendNoMail() {
        publishOtpEvent("bu-bir-json-degil", "alpha-msg-6");

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(rabbitTemplate.receive(DLQ, 500))
                        .as("bozuk mesaj dead-letter kuyruğuna düşmeli")
                        .isNotNull());

        assertThat(mailbox().getReceivedMessages()).isEmpty();
    }

    @Test
    @DisplayName("A7: Gövde bütünlüğü - Teslim edilen mail hem HTML hem düz metin alternatifini içerir")
    void publishOtpEvent_WhenDelivered_ShouldContainHtmlAndPlainTextAlternatives() {
        publishOtpEvent("{\"email\":\"alpha7@shopbridge.com\",\"otpCode\":\"777000\"}", "alpha-msg-7");

        assertThat(mailbox().waitForIncomingEmail(20000, 1)).isTrue();

        String raw = GreenMailUtil.getWholeMessage(mailbox().getReceivedMessages()[0]);
        assertThat(raw).contains("multipart/alternative");
        assertThat(raw).contains("text/plain");
        assertThat(raw).contains("text/html");
        assertThat(raw).contains("777000");
    }
}
