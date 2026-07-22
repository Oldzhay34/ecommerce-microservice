package com.mediaservice.infrastructure.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Bucket olusturma + public-read policy. [Varsayim 8] public-read yalnizca dev icindir;
 * prod'da CDN + signed URL gerekir.
 */
@Component
public class MinioBucketInitializer {

    private static final Logger log = LoggerFactory.getLogger(MinioBucketInitializer.class);

    private final MinioClient minioClient;
    private final String bucket;

    public MinioBucketInitializer(MinioClient minioClient, @Value("${minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket olusturuldu: {}", bucket);
            } else {
                log.info("MinIO bucket zaten mevcut: {}", bucket);
            }
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucket)
                    .config(publicReadPolicy())
                    .build());
            log.info("MinIO bucket public-read policy uygulandi: {}", bucket);
        } catch (Exception e) {
            // Servis ayaga kalkmali; MinIO gec baslamis olabilir. Upload sirasinda hata alinir.
            log.error("MinIO bucket init basarisiz. bucket={}", bucket, e);
        }
    }

    private String publicReadPolicy() {
        return """
               {
                 "Version": "2012-10-17",
                 "Statement": [
                   {
                     "Effect": "Allow",
                     "Principal": {"AWS": ["*"]},
                     "Action": ["s3:GetObject"],
                     "Resource": ["arn:aws:s3:::%s/*"]
                   }
                 ]
               }
               """.formatted(bucket);
    }
}
