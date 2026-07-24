package com.mediaservice.unit.web;

import com.mediaservice.api.dto.ApiErrorResponse;
import com.mediaservice.domain.exception.ImageConversionException;
import com.mediaservice.domain.exception.InvalidReorderRequestException;
import com.mediaservice.domain.exception.MediaAssetNotFoundException;
import com.mediaservice.domain.exception.MediaLimitExceededException;
import com.mediaservice.domain.exception.UnauthorizedMediaAccessException;
import com.mediaservice.domain.exception.UnsupportedMediaFormatException;
import com.mediaservice.infrastructure.web.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT - hata semasi { timestamp, status, error, message, path } tum servislerle
 * ortak sozlesme; her exception -> status eslesmesi burada mekanik olarak dogrulanir.
 */
@DisplayName("UNIT - GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/media/products/x/images");
    }

    @Test
    @DisplayName("U1: UnsupportedMediaFormatException -> 415")
    void handleUnsupportedFormat_ShouldReturn415() {
        assertStatusAndPath(handler.handleUnsupportedFormat(
                new UnsupportedMediaFormatException("kotu format"), request), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    @DisplayName("U2: MaxUploadSizeExceededException -> 413")
    void handleMaxUploadSize_ShouldReturn413() {
        assertStatusAndPath(handler.handleMaxUploadSize(
                new MaxUploadSizeExceededException(5_000_000L), request), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    @DisplayName("U3: MediaLimitExceededException -> 409, mesaj ex.getMessage() ile birebir")
    void handleLimitExceeded_ShouldReturn409WithExceptionMessage() {
        ResponseEntity<ApiErrorResponse> response = handler.handleLimitExceeded(
                new MediaLimitExceededException("maksimum 10 gorsel"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("maksimum 10 gorsel");
    }

    @Test
    @DisplayName("U4: MediaAssetNotFoundException -> 404")
    void handleNotFound_ShouldReturn404() {
        assertStatusAndPath(handler.handleNotFound(
                new MediaAssetNotFoundException(UUID.randomUUID()), request), HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("U5: UnauthorizedMediaAccessException ve AccessDeniedException -> 403")
    void handleUnauthorized_ShouldReturn403ForBothExceptionTypes() {
        assertStatusAndPath(handler.handleUnauthorized(
                new UnauthorizedMediaAccessException("yetki yok"), request), HttpStatus.FORBIDDEN);
        assertStatusAndPath(handler.handleUnauthorized(
                new AccessDeniedException("denied"), request), HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("U6: InvalidReorderRequestException -> 400")
    void handleInvalidReorder_ShouldReturn400() {
        assertStatusAndPath(handler.handleInvalidReorder(
                new InvalidReorderRequestException("eslesmiyor"), request), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("U7: MethodArgumentNotValidException -> 400, field error mesaji kullanilir")
    void handleValidation_ShouldReturn400WithFieldErrorMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("request", "assetIds", "assetIds bos olamaz");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("assetIds bos olamaz");
    }

    @Test
    @DisplayName("U8: ImageConversionException -> 422")
    void handleConversion_ShouldReturn422() {
        assertStatusAndPath(handler.handleConversion(
                new ImageConversionException("islenemedi", new RuntimeException()), request),
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("U9: Beklenmeyen Exception -> 500, govde kullaniciya detay sizdirmaz")
    void handleUnexpected_ShouldReturn500WithGenericMessage() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("stack trace icin gizli detay"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).doesNotContain("stack trace icin gizli detay");
    }

    private void assertStatusAndPath(ResponseEntity<ApiErrorResponse> response, HttpStatus expected) {
        assertThat(response.getStatusCode()).isEqualTo(expected);
        assertThat(response.getBody().getStatus()).isEqualTo(expected.value());
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/media/products/x/images");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
}
