package com.mediaservice.domain.exception;

public class UnauthorizedMediaAccessException extends RuntimeException {

    public UnauthorizedMediaAccessException(String message) {
        super(message);
    }
}