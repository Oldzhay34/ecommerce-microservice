package com.notificationservice.infrastructure.mail;

public final class EmailTemplates {

    private EmailTemplates() {}

    public static final String OTP_HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
            </head>
            <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #FFFFFF; color: #1F2937;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color: #FFFFFF;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" border="0" style="border: 1px solid #DBEAFE; margin-top: 20px;">
                                <tr>
                                    <td align="center" style="background-color: #1E3A8A; padding: 20px;">
                                        <h1 style="color: #FFFFFF; margin: 0; font-size: 24px;">ShopBridge</h1>
                                    </td>
                                </tr>
                                <tr>
                                    <td align="center" style="padding: 40px 20px;">
                                        <p style="font-size: 16px; margin-bottom: 20px;">Merhaba,</p>
                                        <p style="font-size: 16px; margin-bottom: 30px;">İşleminize devam etmek için aşağıdaki doğrulama kodunu kullanabilirsiniz. Lütfen bu kodu ilgili alana manuel olarak giriniz.</p>
                                        <div style="background-color: #DBEAFE; border-radius: 8px; padding: 15px 30px; display: inline-block;">
                                            <span style="font-size: 32px; font-weight: bold; color: #1E3A8A; letter-spacing: 5px;">%s</span>
                                        </div>
                                        <p style="font-size: 12px; color: #6B7280; margin-top: 20px;">Bu kod 10 dakika geçerlidir.</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td align="center" style="background-color: #F9FAFB; padding: 20px; border-top: 1px solid #DBEAFE;">
                                        <p style="font-size: 12px; color: #9CA3AF; margin: 0;">Bu e-posta ShopBridge tarafından otomatik olarak gönderilmiştir, lütfen yanıtlamayınız.</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """;

    public static final String OTP_PLAIN_TEMPLATE = """
            ShopBridge
            
            Merhaba,
            İşleminize devam etmek için aşağıdaki doğrulama kodunu kullanabilirsiniz. Lütfen bu kodu ilgili alana manuel olarak giriniz.
            
            Doğrulama Kodunuz: %s
            
            Bu kod 10 dakika geçerlidir.
            Bu e-posta ShopBridge tarafından otomatik olarak gönderilmiştir, lütfen yanıtlamayınız.
            """;
}