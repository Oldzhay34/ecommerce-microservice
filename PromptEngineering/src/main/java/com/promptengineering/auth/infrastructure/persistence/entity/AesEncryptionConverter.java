package com.promptengineering.auth.infrastructure.persistence.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * PII (name, email) alanlarını AES-256 ile şeffaf şekilde şifreleyen JPA converter.
 *
 * DEĞİŞİKLİK: Anahtar artık System.getenv("AES_SECRET_KEY") ile DEĞİL,
 * Spring @Value ile app.security.aes-secret üzerinden okunuyor.
 *
 * Bu sayede:
 *  - Konfigürasyon tek yerden (application.yml / application-*.yml) yönetiliyor.
 *  - Static blok kalktığı için testlerde ortam değişkeni (env) ayarlama zorunluluğu YOK.
 *  - Converter artık bir Spring bean'i (@Component); Hibernate onu Spring context'inden alır
 *    (Spring Boot, JPA converter'larını bean olarak enjekte edebilir).
 */
@Component
@Converter
public class AesEncryptionConverter implements AttributeConverter<String, String> {

    private static final String ENCRYPTION_ALGORITHM = "AES";

    private final SecretKeySpec keySpec;

    public AesEncryptionConverter(@Value("${app.security.aes-secret}") String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Kritik Hata: app.security.aes-secret tanımlı değil!");
        }
        if (secretKey.getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new IllegalStateException("Kritik Hata: app.security.aes-secret tam olarak 32 byte olmalıdır!");
        }
        this.keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ENCRYPTION_ALGORITHM);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encryptedBytes = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Veritabanına yazılırken veri şifrelenemedi.", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(dbData));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Veritabanından okunurken veri şifresi çözülemedi.", e);
        }
    }
}