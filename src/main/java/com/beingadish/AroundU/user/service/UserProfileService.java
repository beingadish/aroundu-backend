package com.beingadish.AroundU.user.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Manages user profile image upload and deletion for both clients and workers.
 */
public interface UserProfileService {

    /**
     * Uploads a profile image for the given user, replacing any existing one.
     *
     * @param userId the user (client or worker) ID
     * @param file   the image file
     * @return the public URL of the uploaded image
     */
    String uploadProfileImage(Long userId, MultipartFile file);

    /**
     * Deletes the profile image for the given user.
     */
    void deleteProfileImage(Long userId);
}
