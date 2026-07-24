package com.mediaservice.unit.converter;

import com.mediaservice.domain.exception.UnsupportedMediaFormatException;
import com.mediaservice.domain.model.ImageBinary;
import com.mediaservice.domain.model.MediaVariant;
import com.mediaservice.infrastructure.converter.adapter.WebpImageConverter;
import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Katman: UNIT - gercek Scrimage donusumunu (kucuk fixture gorseller uzerinden)
 * kullanir, ancak Spring context / DB / disk yoktur; saf bir bilesen testidir.
 */
@DisplayName("UNIT - WebpImageConverter")
class WebpImageConverterTest {

    private static final int THUMB_WIDTH = 200;
    private static final int MEDIUM_WIDTH = 800;
    private static final int ORIGINAL_MAX_WIDTH = 2000;

    private ExecutorService executor;
    private WebpImageConverter converter;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        converter = new WebpImageConverter(executor, 82, 4, THUMB_WIDTH, MEDIUM_WIDTH, ORIGINAL_MAX_WIDTH);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    @DisplayName("U1: validateFormat - Gecerli PNG/JPEG/WebP magic byte'lariyla hata firlatmaz")
    void validateFormat_WhenValidFormats_ShouldNotThrow() {
        org.assertj.core.api.Assertions.assertThatCode(() ->
                converter.validateFormat("image/png", MediaTestFixtures.validPngBytes())).doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(() ->
                converter.validateFormat("image/jpeg", MediaTestFixtures.validJpegBytes())).doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(() ->
                converter.validateFormat("image/webp", MediaTestFixtures.validWebpBytes())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("U2: validateFormat - Desteklenmeyen Content-Type (ornek: image/gif) 415'e denk dusen exception firlatir")
    void validateFormat_WhenUnsupportedContentType_ShouldThrow() {
        assertThatThrownBy(() -> converter.validateFormat("image/gif", MediaTestFixtures.validPngBytes()))
                .isInstanceOf(UnsupportedMediaFormatException.class);
    }

    @Test
    @DisplayName("U3: validateFormat - Content-Type ile gercek magic byte UYUSMAZSA reddedilir (uzantiya guvenilmez)")
    void validateFormat_WhenDeclaredTypeMismatchesMagicBytes_ShouldThrow() {
        byte[] actuallyPng = MediaTestFixtures.validPngBytes();

        assertThatThrownBy(() -> converter.validateFormat("image/jpeg", actuallyPng))
                .isInstanceOf(UnsupportedMediaFormatException.class);
    }

    @Test
    @DisplayName("U4: validateFormat - Cok kucuk / rastgele byte dizisi reddedilir")
    void validateFormat_WhenGarbageOrTooSmall_ShouldThrow() {
        assertThatThrownBy(() -> converter.validateFormat("image/png", MediaTestFixtures.garbageBytes()))
                .isInstanceOf(UnsupportedMediaFormatException.class);
        assertThatThrownBy(() -> converter.validateFormat("image/png", MediaTestFixtures.tooSmallBytes()))
                .isInstanceOf(UnsupportedMediaFormatException.class);
        assertThatThrownBy(() -> converter.validateFormat(null, MediaTestFixtures.validPngBytes()))
                .isInstanceOf(UnsupportedMediaFormatException.class);
    }

    @Test
    @DisplayName("U5: convertToWebpVariants - Kucuk kaynak gorsel BUYUTULMEZ, orijinal boyut korunur")
    void convertToWebpVariants_WhenSourceSmallerThanTargets_ShouldNotUpscale() {
        Map<MediaVariant, ImageBinary> result = converter.convertToWebpVariants(MediaTestFixtures.validPngBytes());

        assertThat(result).containsKeys(MediaVariant.THUMB, MediaVariant.MEDIUM, MediaVariant.ORIGINAL);
        for (MediaVariant variant : MediaVariant.values()) {
            ImageBinary binary = result.get(variant);
            assertThat(binary.getWidth()).isEqualTo(32);
            assertThat(binary.getHeight()).isEqualTo(24);
            assertThat(binary.getData()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("U6: convertToWebpVariants - Bozuk/decode edilemeyen veri ImageConversionException firlatir")
    void convertToWebpVariants_WhenUndecodable_ShouldThrowImageConversionException() {
        // PNG imzasi (magic byte) var ama govde/IHDR chunk'i EKSIK -> decode kesinlikle patlar.
        byte[] truncated = java.util.Arrays.copyOf(MediaTestFixtures.validPngBytes(), 16);

        assertThatThrownBy(() -> converter.convertToWebpVariants(truncated))
                .isInstanceOf(com.mediaservice.domain.exception.ImageConversionException.class);
    }
}
