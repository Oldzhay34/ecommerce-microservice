package com.mediaservice.infrastructure.web;

import com.mediaservice.api.dto.ApiErrorResponse;
import com.mediaservice.domain.exception.ImageConversionException;
import com.mediaservice.domain.exception.InvalidReorderRequestException;
import com.mediaservice.domain.exception.MediaAssetNotFoundException;
import com.mediaservice.domain.exception.MediaLimitExceededException;
import com.mediaservice.domain.exception.UnauthorizedMediaAccessException;
import com.mediaservice.domain.exception.UnsupportedMediaFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;

/**
 * Hata semasi tum servislerle ORTAK:
 * { timestamp, status, error, message, path }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Desteklenmeyen format / magic byte uyusmazligi -> 415
     */
    @ExceptionHandler(UnsupportedMediaFormatException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedFormat(
            UnsupportedMediaFormatException ex, HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Yalnizca PNG, JPEG ve WebP formatlari kabul edilir.", request);
    }

    /**
     * Dosya 5MB ustu -> 413
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE,
                "Dosya boyutu 5MB sinirini asiyor.", request);
    }

    /**
     * Urun basina 10 gorsel asildi -> 409
     */
    @ExceptionHandler(MediaLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleLimitExceeded(
            MediaLimitExceededException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /**
     * Asset bulunamadi -> 404
     */
    @ExceptionHandler(MediaAssetNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            MediaAssetNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Gorsel bulunamadi.", request);
    }

    /**
     * IDOR / yetkisiz islem -> 403
     */
    @ExceptionHandler({UnauthorizedMediaAccessException.class, AccessDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(
            Exception ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Bu gorsel uzerinde islem yetkiniz yok.", request);
    }

    /**
     * Reorder listesi eksik/fazla, batch > 100 -> 400
     */
    @ExceptionHandler(InvalidReorderRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidReorder(
            InvalidReorderRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Bean Validation (@NotEmpty, @Size max=100) -> 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = "Istek gecersiz.";
        FieldError fieldError = ex.getBindingResult().getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            message = fieldError.getDefaultMessage();
        }
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Donusum hatasi -> 422
     */
    @ExceptionHandler(ImageConversionException.class)
    public ResponseEntity<ApiErrorResponse> handleConversion(
            ImageConversionException ex, HttpServletRequest request) {
        log.error("Gorsel donusum hatasi. path={}", request.getRequestURI(), ex);
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Gorsel islenemedi.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        log.error("Beklenmeyen hata. path={}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Beklenmeyen bir hata olustu.", request);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status,
                                                   String message,
                                                   HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}