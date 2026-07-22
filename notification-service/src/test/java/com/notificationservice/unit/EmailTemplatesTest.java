package com.notificationservice.unit;

import com.notificationservice.infrastructure.mail.EmailTemplates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notification Service - Unit: EmailTemplates render'ı")
class EmailTemplatesTest {

    @Test
    @DisplayName("U13: OTP_HTML_TEMPLATE - OTP kodu gövdeye basılır ve '%%' kaçışı gerçek '%' olarak render edilir")
    void otpHtmlTemplate_WhenFormattedWithOtpCode_ShouldRenderCodeAndEscapedPercent() {
        String html = String.format(EmailTemplates.OTP_HTML_TEMPLATE, "483920");

        assertThat(html).contains("483920");
        assertThat(html).contains("width=\"100%\"");
        assertThat(html).doesNotContain("100%%");
        assertThat(html).doesNotContain("%s");
        assertThat(html).contains("ShopBridge");
        assertThat(html).contains("<!DOCTYPE html>");
    }

    @Test
    @DisplayName("U14: OTP_PLAIN_TEMPLATE - Düz metin sürümünde OTP kodu 'Doğrulama Kodunuz' satırında yer alır")
    void otpPlainTemplate_WhenFormattedWithOtpCode_ShouldRenderCodeInPlainText() {
        String plain = String.format(EmailTemplates.OTP_PLAIN_TEMPLATE, "483920");

        assertThat(plain).contains("Doğrulama Kodunuz: 483920");
        assertThat(plain).doesNotContain("%s");
        assertThat(plain).doesNotContain("<html>");
    }

    @Test
    @DisplayName("U15: Şablonlar - Farklı OTP kodları için render izole edilir (şablon state tutmaz)")
    void templates_WhenFormattedWithDifferentCodes_ShouldNotLeakPreviousCode() {
        String first = String.format(EmailTemplates.OTP_PLAIN_TEMPLATE, "111111");
        String second = String.format(EmailTemplates.OTP_PLAIN_TEMPLATE, "222222");

        assertThat(first).contains("111111").doesNotContain("222222");
        assertThat(second).contains("222222").doesNotContain("111111");
    }

    @Test
    @DisplayName("U16: EmailTemplates - Utility sınıfı olarak final ve private constructor'a sahiptir")
    void emailTemplates_ShouldBeFinalUtilityClassWithPrivateConstructor() throws Exception {
        assertThat(Modifier.isFinal(EmailTemplates.class.getModifiers())).isTrue();

        Constructor<EmailTemplates> constructor = EmailTemplates.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }
}
