package com.example.documents.storage;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class S3SignedUploadService implements ObjectStoragePort {

    private final String bucket;
    private final Duration uploadUrlTtl;
    private final S3Presigner presigner;

    public S3SignedUploadService(
            @Value("${app.storage.bucket}") String bucket,
            @Value("${app.storage.region}") String region,
            @Value("${app.storage.upload-url-ttl}") Duration uploadUrlTtl) {
        this.bucket = bucket;
        this.uploadUrlTtl = uploadUrlTtl;
        this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }

    @Override
    public SignedUploadIntent createUploadIntent(String key, String contentType, long contentLengthBytes) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(contentLengthBytes)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(uploadUrlTtl)
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        return new SignedUploadIntent(
                bucket,
                key,
                presignedRequest.url().toString(),
                Instant.now().plus(uploadUrlTtl));
    }

    @PreDestroy
    void close() {
        presigner.close();
    }
}
