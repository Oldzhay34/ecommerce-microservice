package com.mediaservice.domain.exception;

public class InvalidReorderRequestException extends RuntimeException {

    public InvalidReorderRequestException(String message) {
        super(message);
    }
}