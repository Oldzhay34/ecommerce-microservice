package com.mediaservice.application.port.out;

import com.mediaservice.domain.model.ImageBinary;
import com.mediaservice.domain.model.MediaVariant;

import java.util.Map;

public interface ImageConverterPort {

    /**
     * Content-Type + magic byte dogrulamasi yapar. Uyusmazlikta
     * UnsupportedMediaFormatException firlatir.
     */
    void validateFormat(String declaredContentType, byte[] fileBytes);

    /**
     * 3 varyanti (THUMB/MEDIUM/ORIGINAL) paralel uretir, hepsi WebP.
     */
    Map<MediaVariant, ImageBinary> convertToWebpVariants(byte[] fileBytes);
}