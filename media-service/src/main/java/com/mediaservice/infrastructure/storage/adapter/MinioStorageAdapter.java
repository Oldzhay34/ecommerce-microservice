package com.mediaservice.infrastructure.storage.adapter;

import com.mediaservice.application.port.out.StoragePort;
import com.mediaservice.domain.exception.ImageConversionException;
import com.mediaservice.domain.model.ImageBinary;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

/**
 * StoragePort implementasyonu. AWS S3'e gecis yalnizca bu sinifi degistirir.
 */
@Component
public class MinioStorageAdapter implements StoragePort {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageAdapter.class);
    private static final String WEBP_CONTENT_TYPE = "image/webp";

    private final MinioClient minioClient;
    private final String bucket;
    private final String publicBaseUrl;

    public MinioStorageAdapter(MinioClient minioClient,
                               @Value("${minio.bucket}") String bucket,
                               @Value("${media.public-base-url}") String publicBaseUrl) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.publicBaseUrl = stripTrailingSlash(publicBaseUrl);
    }

    @Override
    public String upload(String storageKey, ImageBinary binary) {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(binary.getData())) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(storageKey)
                    .stream(stream, binary.getSizeBytes(), -1)
                    .contentType(WEBP_CONTENT_TYPE)
                    .build());
            return buildPublicUrl(storageKey);
        } catch (Exception e) {
            log.error("MinIO upload hatasi. key={}", storageKey, e);
            throw new ImageConversionException("Gorsel islenemedi.", e);
        }
    }

    @Override
    public String buildPublicUrl(String storageKey) {
        return publicBaseUrl + "/" + storageKey;
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}