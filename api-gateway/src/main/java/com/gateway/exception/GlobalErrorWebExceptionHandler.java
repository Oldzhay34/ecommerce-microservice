package com.gateway.exception;

import com.gateway.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Merkezi hata yakalayici.
 * - Downstream erisilemez (ConnectException / timeout / IOException) -> 503
 * - ResponseStatusException (ornegin rate limit 429) -> ilgili status
 * - Diger beklenmeyen hatalar -> 500
 * Hepsi ErrorResponse JSON formatinda doner.
 *
 * @Order(-2): Spring Boot'un ErrorWebFluxAutoConfiguration'i kendi
 * DefaultErrorWebExceptionHandler bean'ini @Order(-1) ile kaydeder. Bu sinif da -1
 * olsaydi iki isleyici AYNI onceligi paylasir ve hangisinin once calisacagi bean
 * kesif sirasina kalirdi (hata govdesi bazen Boot'un varsayilan formatinda donerdi).
 * -2 vererek ErrorResponse formatinin her zaman kazanmasi garanti edilir.
 */
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status;
        String message;

        if (ex instanceof ResponseStatusException rse) {
            HttpStatus resolved = HttpStatus.resolve(rse.getStatusCode().value());
            status = resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
            if (status == HttpStatus.TOO_MANY_REQUESTS) {
                message = "Istek limiti asildi. Lutfen biraz sonra tekrar deneyin.";
            } else {
                message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
            }
        } else if (ex instanceof ConnectException || ex instanceof IOException
                || ex.getClass().getSimpleName().contains("Timeout")) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Hedef servise su anda ulasilamiyor. Lutfen daha sonra tekrar deneyin.";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Gateway tarafinda beklenmeyen bir hata olustu.";
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                message,
                exchange.getRequest().getURI().getPath()
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"status\":" + status.value() + ",\"message\":\"" + message + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
