package com.beingadish.AroundU.user.controller;

import com.beingadish.AroundU.common.dto.ApiResponse;
import com.beingadish.AroundU.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.beingadish.AroundU.common.constants.URIConstants.USER_BASE;

/**
 * Handles profile image upload / deletion for any authenticated user. The
 * endpoint supports both CLIENT and WORKER roles.
 */
@RestController
@RequestMapping(USER_BASE)
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Profile image management for clients and workers")
public class UserProfileController {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB

    private final UserProfileService userProfileService;

    @PostMapping(value = "/{userId}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @Operation(summary = "Upload profile image",
            description = "Upload a profile image (JPEG/PNG, max 5 MB). Replaces any existing image.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image uploaded"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "File too large")
    })
    public ResponseEntity<ApiResponse<String>> uploadProfileImage(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Image file (JPEG/PNG, max 5 MB)") @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("File is empty"));
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            return ResponseEntity.status(413).body(ApiResponse.error("File exceeds 5 MB limit"));
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Only JPEG and PNG images are accepted"));
        }

        String imageUrl = userProfileService.uploadProfileImage(userId, file);
        return ResponseEntity.ok(ApiResponse.success(imageUrl));
    }

    @DeleteMapping("/{userId}/profile-image")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @Operation(summary = "Delete profile image",
            description = "Removes the user's profile image.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No image to delete")
    })
    public ResponseEntity<ApiResponse<String>> deleteProfileImage(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        userProfileService.deleteProfileImage(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile image deleted"));
    }
}
