package com.mediaservice.domain.model;

public enum MediaVariant {
    THUMB("thumb"),
    MEDIUM("medium"),
    ORIGINAL("original");

    private final String suffix;

    MediaVariant(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }
}