package com.notificationservice.infrastructure.mail;

import com.notificationservice.application.port.out.EmailSenderPort;
import com.notificationservice.domain.exception.EmailSendException;
import com.notificationservice.domain.model.OtpEmailPayload;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender javaMailSender;
    private static final String SUBJECT = "ShopBridge - Doğrulama Kodunuz";

    public SmtpEmailSenderAdapter(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendOtpEmail(OtpEmailPayload payload) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(payload.getEmail());
            helper.setSubject(SUBJECT);

            String plainTextBody = String.format(EmailTemplates.OTP_PLAIN_TEMPLATE, payload.getOtpCode());
            String htmlBody = String.format(EmailTemplates.OTP_HTML_TEMPLATE, payload.getOtpCode());

            // DİKKAT: setText(plain,false) + setText(html,true) şeklinde iki ayrı çağrı
            // aynı gövde parçasını EZER; düz metin alternatifi mail'e hiç eklenmez.
            // multipart/alternative üretmek için iki gövde TEK çağrıda verilmelidir.
            helper.setText(plainTextBody, htmlBody);

            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new EmailSendException("Failed to send OTP email to: " + payload.getEmail(), e);
        }
    }
}