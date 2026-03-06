package com.beingadish.AroundU.infrastructure.storage.impl;

import com.beingadish.AroundU.infrastructure.metrics.MetricsService;
import com.beingadish.AroundU.infrastructure.storage.ImageStorageService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Image storage service with Resilience4j circuit breaker + retry + local
 * fallback.
 * <p>
 * Execution order: {@code CircuitBreaker(Retry(s3Upload))}.
 * <ul>
 * <li>If S3 is healthy → upload directly and return the S3 URL.</li>
 * <li>If retries exhaust → store locally, queue async S3 upload, serve from
 * local.</li>
 * <li>If the circuit is open → fast-fail to local storage.</li>
 * </ul>
 */
@Service
@Slf4j
public class ImageStorageServiceImpl implements ImageStorageService {

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    /**
     * Simulates local storage for fallback (in production: temp disk path).
     * -- GETTER --
     * Returns local storage map (visible for tests).
     */
    @Getter
    private final Map<String, byte[]> localStorage = new ConcurrentHashMap<>();

    /**
     * Queue of file names awaiting async upload to S3.
     * -- GETTER --
     * Returns pending S3 upload queue (visible for monitoring / tests).
     */
    @Getter
    private final Queue<String> pendingS3Uploads = new ConcurrentLinkedQueue<>();

    public ImageStorageServiceImpl(@Qualifier("imageUploadCircuitBreaker") CircuitBreaker circuitBreaker,
                                   @Qualifier("imageUploadRetry") Retry retry,
                                   MetricsService metricsService) {
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
    }

    @Override
    public String uploadImage(String fileName, byte[] data) {
        Supplier<String> decorated = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                Retry.decorateSupplier(retry, () -> doUploadToS3(fileName, data))
        );

        try {
            String url = decorated.get();
            log.debug("Image uploaded to S3: {}", url);
            return url;
        } catch (Exception e) {
            log.warn("S3 upload failed for '{}', storing locally and queuing: {}",
                    fileName, e.getMessage());
            return storeLocally(fileName, data);
        }
    }

    @Override
    public String getImageUrl(String fileName) {
        if (localStorage.containsKey(fileName)) {
            return "/local-images/" + fileName;
        }
        // In production: return pre-signed S3 URL
        return "https://s3.example.com/aroundu-images/" + fileName;
    }

    @Override
    public void deleteImage(String fileName) {
        localStorage.remove(fileName);
        pendingS3Uploads.remove(fileName);
        // TODO: also delete from S3 with resilience wrapping
        log.info("Deleted image: {}", fileName);
    }

    // ── Internals ────────────────────────────────────────────────────────

    /**
     * Actual S3 upload. Replace with real AWS SDK call.
     */
    private String doUploadToS3(String fileName, byte[] data) {
        // TODO: replace with real S3 client (PutObjectRequest)
        log.info("Uploading to S3: {} ({} bytes)", fileName, data.length);
        // Simulated success
        return "https://s3.example.com/aroundu-images/" + fileName;
    }

    private String storeLocally(String fileName, byte[] data) {
        localStorage.put(fileName, data);
        pendingS3Uploads.offer(fileName);
        log.info("Stored locally and queued for S3 upload (pending={}): {}",
                pendingS3Uploads.size(), fileName);
        return "/local-images/" + fileName;
    }

}
