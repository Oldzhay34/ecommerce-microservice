package com.notificationservice.integration;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.notificationservice.domain.exception.EmailSendException;
import com.notificationservice.domain.model.OtpEmailPayload;
import com.notificationservice.infrastructure.mail.SmtpEmailSenderAdapter;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Adapter entegrasyon testi: SmtpEmailSenderAdapter GERÇEK bir SMTP sunucusuna
 * (GreenMail, in-process) karşı çalıştırılır. Docker gerektirmez.
 */
@DisplayName("Notification Service - Integration: SmtpEmailSenderAdapter x GreenMail SMTP")
class SmtpEmailSenderAdapterGreenMailIntegrationTest {

    @RegisterExtension
    static final GreenMailExtension GREENMAIL = new GreenMailExtension(ServerSetupTest.SMTP)
            .withPerMethodLifecycle(true);

    private SmtpEmailSenderAdapter adapter;

    @BeforeEach
    void setUp() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("127.0.0.1");
        mailSender.setPort(GREENMAIL.getSmtp().getPort());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");
        props.put("mail.smtp.starttls.enable", "false");

        adapter = new SmtpEmailSenderAdapter(mailSender);
    }

    private static String bodyOf(MimeMessage message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String text) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        collect((Multipart) content, sb);
        return sb.toString();
    }

    private static void collect(Multipart multipart, StringBuilder sb) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            Object part = multipart.getBodyPart(i).getContent();
            if (part instanceof MimeMultipart nested) {
                collect(nested, sb);
            } else {
                sb.append(part).append("\n");
            }
        }
    }

    @Test
    @DisplayName("I1: sendOtpEmail - Mail gerçekten SMTP sunucusuna teslim edilir; alıcı ve konu doğrudur")
    void sendOtpEmail_WhenSmtpAvailable_ShouldDeliverMailWithCorrectRecipientAndSubject() throws Exception {
        adapter.sendOtpEmail(new OtpEmailPayload("customer@shopbridge.com", "483920"));

        assertThat(GREENMAIL.waitForIncomingEmail(5000, 1)).isTrue();

        MimeMessage[] received = GREENMAIL.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("customer@shopbridge.com");
        assertThat(received[0].getSubject()).isEqualTo("ShopBridge - Doğrulama Kodunuz");
    }

    @Test
    @DisplayName("I2: sendOtpEmail - Mail gövdesindeki HTML sürümünde OTP kodu ve ShopBridge markası yer alır")
    void sendOtpEmail_WhenDelivered_ShouldContainOtpCodeInHtmlBody() throws Exception {
        adapter.sendOtpEmail(new OtpEmailPayload("customer@shopbridge.com", "135790"));

        assertThat(GREENMAIL.waitForIncomingEmail(5000, 1)).isTrue();
        String body = bodyOf(GREENMAIL.getReceivedMessages()[0]);

        assertThat(body).contains("135790");
        assertThat(body).contains("ShopBridge");
        assertThat(body).contains("<!DOCTYPE html>");
        assertThat(body).doesNotContain("%s");
    }

    @Test
    @DisplayName("I3: sendOtpEmail - Mail multipart/alternative olarak gönderilir; düz metin sürümü de OTP kodunu içerir")
    void sendOtpEmail_WhenDelivered_ShouldAlsoIncludePlainTextAlternativeWithOtpCode() throws Exception {
        adapter.sendOtpEmail(new OtpEmailPayload("customer@shopbridge.com", "246813"));

        assertThat(GREENMAIL.waitForIncomingEmail(5000, 1)).isTrue();
        String body = bodyOf(GREENMAIL.getReceivedMessages()[0]);

        assertThat(body)
                .as("HTML göstermeyen istemciler için düz metin alternatifi kaybolmamalı")
                .contains("Doğrulama Kodunuz: 246813");
    }

    @Test
    @DisplayName("I4: sendOtpEmail - Türkçe karakterler UTF-8 olarak bozulmadan iletilir")
    void sendOtpEmail_WhenBodyContainsTurkishCharacters_ShouldPreserveUtf8Encoding() throws Exception {
        adapter.sendOtpEmail(new OtpEmailPayload("customer@shopbridge.com", "999111"));

        assertThat(GREENMAIL.waitForIncomingEmail(5000, 1)).isTrue();
        MimeMessage message = GREENMAIL.getReceivedMessages()[0];

        assertThat(message.getSubject()).isEqualTo("ShopBridge - Doğrulama Kodunuz");
        assertThat(bodyOf(message)).contains("İşleminize devam etmek için");
    }

    @Test
    @DisplayName("I5: sendOtpEmail - SMTP sunucusu erişilemezse hata yayılır ve hiçbir mail teslim edilmez")
    void sendOtpEmail_WhenSmtpUnreachable_ShouldFailAndDeliverNothing() {
        JavaMailSenderImpl broken = new JavaMailSenderImpl();
        broken.setHost("127.0.0.1");
        broken.setPort(1); // kapalı port
        broken.getJavaMailProperties().put("mail.smtp.timeout", "1000");
        broken.getJavaMailProperties().put("mail.smtp.connectiontimeout", "1000");

        SmtpEmailSenderAdapter brokenAdapter = new SmtpEmailSenderAdapter(broken);

        assertThatThrownBy(() -> brokenAdapter.sendOtpEmail(new OtpEmailPayload("customer@shopbridge.com", "000000")))
                .isInstanceOf(MailSendException.class);

        assertThat(GREENMAIL.getReceivedMessages()).isEmpty();
    }

    @Test
    @DisplayName("I6: sendOtpEmail - Geçersiz alıcı adresi SMTP'ye hiç gitmeden EmailSendException'a sarılır")
    void sendOtpEmail_WhenRecipientMalformed_ShouldThrowEmailSendExceptionBeforeReachingSmtp() {
        assertThatThrownBy(() -> adapter.sendOtpEmail(new OtpEmailPayload("bozuk adres @@", "123456")))
                .isInstanceOf(EmailSendException.class);

        assertThat(GREENMAIL.getReceivedMessages()).isEmpty();
    }
}
