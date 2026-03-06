package com.beingadish.AroundU.user.service.impl;

import com.beingadish.AroundU.infrastructure.storage.ImageStorageService;
import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.entity.Worker;
import com.beingadish.AroundU.user.repository.ClientRepository;
import com.beingadish.AroundU.user.repository.WorkerRepository;
import com.beingadish.AroundU.user.service.UserProfileService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the user as either a Client or Worker and delegates image storage to
 * {@link ImageStorageService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private final ClientRepository clientRepository;
    private final WorkerRepository workerRepository;
    private final ImageStorageService imageStorageService;

    @Override
    public String uploadProfileImage(Long userId, MultipartFile file) {
        // Determine file extension
        String originalName = file.getOriginalFilename();
        String extension = "jpg";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        }

        String fileName = "profile/" + userId + "/" + UUID.randomUUID() + "." + extension;

        try {
            String imageUrl = imageStorageService.uploadImage(fileName, file.getBytes());
            // Update the user entity
            updateProfileImageUrl(userId, imageUrl);
            log.info("Profile image uploaded for user {}: {}", userId, imageUrl);
            return imageUrl;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read image file", e);
        }
    }

    @Override
    public void deleteProfileImage(Long userId) {
        String currentUrl = getProfileImageUrl(userId);
        if (currentUrl == null || currentUrl.isBlank()) {
            throw new EntityNotFoundException("No profile image found for user " + userId);
        }

        imageStorageService.deleteImage(currentUrl);
        updateProfileImageUrl(userId, null);
        log.info("Profile image deleted for user {}", userId);
    }

    private void updateProfileImageUrl(Long userId, String url) {
        // Try client first, then worker
        Optional<Client> client = clientRepository.findById(userId);
        if (client.isPresent()) {
            client.get().setProfileImageUrl(url);
            clientRepository.save(client.get());
            return;
        }

        Optional<Worker> worker = workerRepository.findById(userId);
        if (worker.isPresent()) {
            worker.get().setProfileImageUrl(url);
            workerRepository.save(worker.get());
            return;
        }

        throw new EntityNotFoundException("User not found: " + userId);
    }

    private String getProfileImageUrl(Long userId) {
        Optional<Client> client = clientRepository.findById(userId);
        if (client.isPresent()) {
            return client.get().getProfileImageUrl();
        }

        Optional<Worker> worker = workerRepository.findById(userId);
        if (worker.isPresent()) {
            return worker.get().getProfileImageUrl();
        }

        throw new EntityNotFoundException("User not found: " + userId);
    }
}
