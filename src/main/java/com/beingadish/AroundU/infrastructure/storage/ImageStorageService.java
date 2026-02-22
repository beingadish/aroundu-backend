package com.beingadish.AroundU.infrastructure.storage;

/**
 * Abstraction for uploading/retrieving images (e.g. S3 / local file-system).
 * <p>
 * Production implementation wraps S3 calls with a Resilience4j circuit breaker
 * and retry; if S3 is unavailable, images are stored locally and queued for
 * async upload when the circuit closes.
 */
public interface ImageStorageService {

    /**
     * Upload an image.
     *
     * @param fileName unique file name / key
     * @param data     raw image bytes
     * @return public URL or local path where the image can be served
     */
    String uploadImage(String fileName, byte[] data);

    /**
     * Retrieve the serving URL for a previously uploaded image.
     */
    String getImageUrl(String fileName);

    /**
     * Delete an image.
     */
    void deleteImage(String fileName);
}
