package com.mediaservice.infrastructure.converter.adapter;

import com.mediaservice.application.port.out.ImageConverterPort;
import com.mediaservice.domain.exception.ImageConversionException;
import com.mediaservice.domain.exception.UnsupportedMediaFormatException;
import com.mediaservice.domain.model.ImageBinary;
import com.mediaservice.domain.model.MediaVariant;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

/**
 * ImageConverterPort implementasyonu.
 *
 * Kurallar:
 *  - Kabul: image/png, image/jpeg, image/webp. Bunun disi -> 415.
 *  - Magic byte dogrulamasi: uzantiya/Content-Type header'ina GUVENILMEZ.
 *  - Cikti: HER ZAMAN WebP. [Karar B/1] WebP yuklense bile decode edilip yeniden encode edilir
 *    (tutarlilik > kalite; kullanici onayi alindi).
 *  - 3 varyant CompletableFuture ile PARALEL uretilir.
 */
@Component
public class WebpImageConverter implements ImageConverterPort {

    private static final Logger log = LoggerFactory.getLogger(WebpImageConverter.class);

    private static final Set<String> ACCEPTED_CONTENT_TYPES =
            Set.of("image/png", "image/jpeg", "image/webp");

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MAGIC = {0x57, 0x45, 0x42, 0x50};

    private static final String FORMAT_ERROR = "Yalnizca PNG, JPEG ve WebP formatlari kabul edilir.";

    private final ExecutorService conversionExecutor;
    private final int quality;
    private final int method;
    private final int thumbWidth;
    private final int mediumWidth;
    private final int originalMaxWidth;

    public WebpImageConverter(@Qualifier("conversionExecutor") ExecutorService conversionExecutor,
                              @Value("${media.webp.quality}") int quality,
                              @Value("${media.webp.method}") int method,
                              @Value("${media.variant.thumb-width}") int thumbWidth,
                              @Value("${media.variant.medium-width}") int mediumWidth,
                              @Value("${media.variant.original-max-width}") int originalMaxWidth) {
        this.conversionExecutor = conversionExecutor;
        this.quality = quality;
        this.method = method;
        this.thumbWidth = thumbWidth;
        this.mediumWidth = mediumWidth;
        this.originalMaxWidth = originalMaxWidth;
    }

    @Override
    public void validateFormat(String declaredContentType, byte[] fileBytes) {
        if (declaredContentType == null || !ACCEPTED_CONTENT_TYPES.contains(normalize(declaredContentType))) {
            throw new UnsupportedMediaFormatException(FORMAT_ERROR);
        }
        if (fileBytes == null || fileBytes.length < 12) {
            throw new UnsupportedMediaFormatException(FORMAT_ERROR);
        }
        String detected = detectByMagicBytes(fileBytes);
        if (detected == null) {
            throw new UnsupportedMediaFormatException(FORMAT_ERROR);
        }
        if (!detected.equals(normalize(declaredContentType))) {
            log.warn("Content-Type uyusmazligi: declared={}, magicByte={}", declaredContentType, detected);
            throw new UnsupportedMediaFormatException(FORMAT_ERROR);
        }
    }

    private String normalize(String contentType) {
        int idx = contentType.indexOf(';');
        String base = idx >= 0 ? contentType.substring(0, idx) : contentType;
        return base.trim().toLowerCase();
    }

    private String detectByMagicBytes(byte[] bytes) {
        if (startsWith(bytes, PNG_MAGIC, 0)) {
            return "image/png";
        }
        if (startsWith(bytes, JPEG_MAGIC, 0)) {
            return "image/jpeg";
        }
        // WebP: "RIFF" + 4 byte length + "WEBP"
        if (startsWith(bytes, RIFF_MAGIC, 0) && startsWith(bytes, WEBP_MAGIC, 8)) {
            return "image/webp";
        }
        return null;
    }

    private boolean startsWith(byte[] source, byte[] prefix, int offset) {
        if (source.length < offset + prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (source[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Map<MediaVariant, ImageBinary> convertToWebpVariants(byte[] fileBytes) {
        ImmutableImage source = decode(fileBytes);
        int originalWidth = source.width;
        int originalHeight = source.height;

        Map<MediaVariant, Integer> targetWidths = new EnumMap<>(MediaVariant.class);
        targetWidths.put(MediaVariant.THUMB, resolveTargetWidth(originalWidth, thumbWidth));
        targetWidths.put(MediaVariant.MEDIUM, resolveTargetWidth(originalWidth, mediumWidth));
        targetWidths.put(MediaVariant.ORIGINAL, resolveTargetWidth(originalWidth, originalMaxWidth));

        Map<MediaVariant, CompletableFuture<ImageBinary>> futures = new HashMap<>();
        for (Map.Entry<MediaVariant, Integer> entry : targetWidths.entrySet()) {
            MediaVariant variant = entry.getKey();
            int targetWidth = entry.getValue();
            int targetHeight = computeHeight(originalWidth, originalHeight, targetWidth);
            futures.put(variant, CompletableFuture.supplyAsync(
                    () -> encodeVariant(source, variant, targetWidth, targetHeight),
                    conversionExecutor));
        }

        try {
            CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
                    .join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new ImageConversionException("Gorsel islenemedi.", cause);
        }

        Map<MediaVariant, ImageBinary> result = new EnumMap<>(MediaVariant.class);
        for (Map.Entry<MediaVariant, CompletableFuture<ImageBinary>> entry : futures.entrySet()) {
            result.put(entry.getKey(), entry.getValue().join());
        }
        return result;
    }

    private ImmutableImage decode(byte[] fileBytes) {
        try {
            return ImmutableImage.loader().fromStream(new ByteArrayInputStream(fileBytes));
        } catch (Exception e) {
            throw new ImageConversionException("Gorsel islenemedi.", e);
        }
    }

    /**
     * Orijinal genislik hedeften kucukse BUYUTME YAPILMAZ - orijinal genislik kullanilir.
     * ORIGINAL varyantinda hedef = originalMaxWidth (2000) oldugu icin ayni kural clamp gorevi gorur.
     */
    private int resolveTargetWidth(int originalWidth, int desiredWidth) {
        return Math.min(originalWidth, desiredWidth);
    }

    private int computeHeight(int originalWidth, int originalHeight, int targetWidth) {
        if (originalWidth == targetWidth) {
            return originalHeight;
        }
        return (int) Math.round(originalHeight * (targetWidth / (double) originalWidth));
    }

    private ImageBinary encodeVariant(ImmutableImage source, MediaVariant variant, int targetWidth, int targetHeight) {
        try {
            ImmutableImage scaled = (source.width == targetWidth && source.height == targetHeight)
                    ? source
                    : source.scaleTo(targetWidth, targetHeight);

            WebpWriter writer = WebpWriter.DEFAULT
                    .withQ(quality)
                    .withM(method)
                    .withLossless();

            byte[] bytes = scaled.bytes(writer);
            return new ImageBinary(bytes, scaled.width, scaled.height, variant);
        } catch (Exception e) {
            log.error("WebP encode hatasi. variant={}, targetWidth={}", variant, targetWidth, e);
            throw new ImageConversionException("Gorsel islenemedi.", e);
        }
    }
}