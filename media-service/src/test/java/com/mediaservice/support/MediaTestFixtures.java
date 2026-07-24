package com.mediaservice.support;

import com.mediaservice.domain.model.MediaAsset;
import com.mediaservice.domain.model.MediaStatus;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.nio.JpegWriter;
import com.sksamuel.scrimage.webp.WebpWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.ImageIO;

/**
 * media-service'e OZEL test fixture'lari. Gercek, decode edilebilir kucuk gorseller
 * uretir (magic byte + gercek piksel verisi) ki hem format dogrulama hem de gercek
 * WebP donusum akisi (Scrimage) ayni fixture uzerinden test edilebilsin.
 */
public final class MediaTestFixtures {

    private MediaTestFixtures() {
    }

    private static final int FIXTURE_WIDTH = 32;
    private static final int FIXTURE_HEIGHT = 24;

    /** Gercek, decode edilebilir kucuk bir PNG (magic byte + gecerli piksel verisi). */
    public static byte[] validPngBytes() {
        return encode(image(), "png");
    }

    /** Gercek, decode edilebilir kucuk bir JPEG. */
    public static byte[] validJpegBytes() {
        try {
            return image().bytes(JpegWriter.Default);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Gercek, decode edilebilir kucuk bir WebP (kabul edilen tek "zaten webp" girdi). */
    public static byte[] validWebpBytes() {
        try {
            return image().bytes(WebpWriter.DEFAULT);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Magic byte PNG olan ama Content-Type'i JPEG iddia eden bozuk istek. */
    public static byte[] pngBytesWithMismatchedDeclaredType() {
        return validPngBytes();
    }

    /** Ne PNG ne JPEG ne WebP - format dogrulamasinin 415 dondurdugunu kanitlamak icin. */
    public static byte[] garbageBytes() {
        byte[] bytes = new byte[64];
        ThreadLocalRandom.current().nextBytes(bytes);
        // Herhangi bir gercek magic byte imzasiyla CAKISMADIGINDAN emin ol.
        bytes[0] = 0x00;
        bytes[1] = 0x00;
        bytes[2] = 0x00;
        bytes[3] = 0x00;
        return bytes;
    }

    public static byte[] tooSmallBytes() {
        return new byte[]{1, 2, 3};
    }

    private static ImmutableImage image() {
        return ImmutableImage.filled(FIXTURE_WIDTH, FIXTURE_HEIGHT, new java.awt.Color(120, 60, 200));
    }

    private static byte[] encode(ImmutableImage image, String formatName) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image.awt(), formatName, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ------------------------------------------------------------------
    // Domain fixture'lari
    // ------------------------------------------------------------------

    public static MediaAsset activeAsset(UUID id, UUID productId, UUID storeId, int sortOrder, boolean primary) {
        return new MediaAsset(
                id, productId, storeId,
                "https://cdn.test/" + id + "_medium.webp",
                "https://cdn.test/" + id + "_thumb.webp",
                "https://cdn.test/" + id + "_original.webp",
                "products/" + productId + "/" + id + "_medium.webp",
                "products/" + productId + "/" + id + "_thumb.webp",
                "products/" + productId + "/" + id + "_medium.webp",
                "products/" + productId + "/" + id + "_original.webp",
                "image/webp",
                12_345L, 800, 600,
                sortOrder, primary, MediaStatus.ACTIVE,
                Instant.now(), 0L);
    }

    public static MediaAsset deletedAsset(UUID id, UUID productId, UUID storeId) {
        MediaAsset asset = activeAsset(id, productId, storeId, 0, false);
        asset.markDeleted();
        return asset;
    }

    /**
     * product-service'in (yayinlarsa) product.exchange / product.deleted uzerine
     * basacagi olay govdesi - ProductDeletedEventListener'in bekledigi semaya birebir uyar.
     */
    public static String productDeletedPayload(UUID productId) {
        return String.format("""
                {"eventId":"%s","eventType":"ProductDeletedEvent","occurredAt":"%s","productId":"%s"}
                """, UUID.randomUUID(), Instant.now(), productId);
    }

    public static String productDeletedPayloadMissingProductId() {
        return String.format("""
                {"eventId":"%s","eventType":"ProductDeletedEvent","occurredAt":"%s"}
                """, UUID.randomUUID(), Instant.now());
    }

    public static String productDeletedPayloadInvalidProductId() {
        return String.format("""
                {"eventId":"%s","eventType":"ProductDeletedEvent","occurredAt":"%s","productId":"not-a-uuid"}
                """, UUID.randomUUID(), Instant.now());
    }
}
