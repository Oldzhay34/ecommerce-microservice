package com.mediaservice.domain.exception;

import java.util.UUID;

public class MediaAssetNotFoundException extends RuntimeException {

    public MediaAssetNotFoundException(UUID assetId) {
        super("Gorsel bulunamadi: " + assetId);
    }

    public MediaAssetNotFoundException(String message) {
        super(message);
    }
}