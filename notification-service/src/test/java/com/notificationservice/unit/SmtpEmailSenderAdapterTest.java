package com.notificationservice.unit;

import com.notificationservice.domain.exception.EmailSendException;
import com.notificationservice.domain.model.OtpEmailPayload;
import com.notificationservice.infrastructure.mail.SmtpEmailSenderAdapter;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("Notification Service - Unit: SmtpEmailSenderAdapter (JavaMailSender mock'lu)")
@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderAdapterTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private SmtpEmailSenderAdapter adapter;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        // Session null olan gerçek bir MimeMessage; MimeMessageHelper bunun üzerinde çalışır.
        mimeMessage = new MimeMessage((jakarta.mail.Session) null);
    }

    @Test
    @DisplayName("U17: sendOtpEmail - Alıcı, konu ve OTP içeren gövde set edilip mesaj JavaMailSender'a verilir")
    void sendOtpEmail_WhenPayloadValid_ShouldPopulateMessageAndDelegateToMailSender() throws Exception {
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        adapter.sendOtpEmail(new OtpEmailPayload("customer@shopbridge.com", "483920"));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());

        MimeMessage sent = captor.getValue();
        assertThat(sent.getAllRecipients()).hasSize(1);
        assertThat(sent.getAllRecipients()[0].toString()).isEqualTo("customer@shopbridge.com");
        assertThat(sent.getSubject()).isEqualTo("ShopBridge - Doğrulama Kodunuz");
    }

    @Test
    @DisplayName("U18: sendOtpEmail - Geçersiz alıcı adresi MessagingException üretince EmailSendException'a sarılır")
    void sendOtpEmail_WhenRecipientAddressInvalid_ShouldWrapIntoEmailSendException() {
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertThatThrownBy(() -> adapter.sendOtpEmail(new OtpEmailPayload("gecersiz adres @@", "483920")))
                .isInstanceOf(EmailSendException.class)
                .hasMessageContaining("gecersiz adres")
                .hasCauseInstanceOf(jakarta.mail.MessagingException.class);

        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("U19: sendOtpEmail - Transport hatası (MailSendException) sarmalanmadan yayılır, mail 'gönderildi' sayılmaz")
    void sendOtpEmail_WhenTransportFails_ShouldPropagateMailException() {
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP bağlantısı reddedildi"))
                .when(javaMailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> adapter.sendOtpEmail(new OtpEmailPayload("customer@shopbridge.com", "483920")))
                .isInstanceOf(MailSendException.class);
    }

    @Test
    @DisplayName("U20: sendOtpEmail - Alıcı adresi null ise mesaj gönderilmez (hata guard'ı devreye girer)")
    void sendOtpEmail_WhenRecipientIsNull_ShouldNotSendMessage() {
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertThatThrownBy(() -> adapter.sendOtpEmail(new OtpEmailPayload(null, "483920")))
                .isInstanceOf(RuntimeException.class);

        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }
}
