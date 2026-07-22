package com.gateway.dto;

import java.time.LocalDateTime;

/**
 * Standart hata yaniti. Tum gateway hatalari (401/429/503 vb.) bu formatta doner.
 * CODING_STYLE_GUIDELINES: Lombok kullanilmaz, getter/setter/constructor manuel yazilir.
 */
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String message;
    private String path;

    public ErrorResponse() {
    }

    public ErrorResponse(LocalDateTime timestamp, int status, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.message = message;
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
