package com.mediaservice.domain.exception;

/**
 * Kabul edilmeyen Content-Type veya magic byte uyusmazliginda firlatilir.
 * GlobalExceptionHandler tarafindan 415 Unsupported Media Type'a maplenir.
 *
 * Firlatildigi yerler (WebpImageConverter.validateFormat):
 *  1. declaredContentType null veya {image/png, image/jpeg, image/webp} disinda
 *  2. Dosya 12 byte'tan kisa (magic byte okunamaz)
 *  3. Magic byte hicbir kabul edilen formata uymuyor
 *  4. Magic byte ile declared Content-Type CELISIYOR (orn. GIF dosyasi image/png diye gonderilmis)
 */
public class UnsupportedMediaFormatException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Yalnizca PNG, JPEG ve WebP formatlari kabul edilir.";

    public UnsupportedMediaFormatException() {
        super(DEFAULT_MESSAGE);
    }

    public UnsupportedMediaFormatException(String message) {
        super(message);
    }

    public UnsupportedMediaFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}