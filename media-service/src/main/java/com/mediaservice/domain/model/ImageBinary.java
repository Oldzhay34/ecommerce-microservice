package com.mediaservice.domain.model;

/**
 * Bellekte tutulan, WebP'ye donusturulmus tek bir varyantin ikili verisi.
 * Saf POJO - framework bagimsiz, Lombok yok.
 * [Varsayim 7] Prompt'ta alan tablosu verilmemisti; data/width/height/variant varsayildi.
 */
public class ImageBinary {

    private final byte[] data;
    private final int width;
    private final int height;
    private final MediaVariant variant;

    public ImageBinary(byte[] data, int width, int height, MediaVariant variant) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("ImageBinary.data bos olamaz");
        }
        if (variant == null) {
            throw new IllegalArgumentException("ImageBinary.variant null olamaz");
        }
        this.data = data;
        this.width = width;
        this.height = height;
        this.variant = variant;
    }

    public byte[] getData() {
        return data;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public MediaVariant getVariant() {
        return variant;
    }

    public long getSizeBytes() {
        return data.length;
    }
}