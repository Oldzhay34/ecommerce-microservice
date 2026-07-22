package com.mediaservice.domain.exception;

public class MediaLimitExceededException extends RuntimeException {

    public MediaLimitExceededException(String message) {
        super(message);
    }
}