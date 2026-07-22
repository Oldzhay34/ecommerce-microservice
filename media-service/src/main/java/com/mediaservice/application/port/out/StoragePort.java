package com.mediaservice.application.port.out;

import com.mediaservice.domain.model.ImageBinary;

public interface StoragePort {

    /**
     * Verilen binary'yi object storage'a yazar ve tam public URL doner.
     */
    String upload(String storageKey, ImageBinary binary);

    String buildPublicUrl(String storageKey);
}