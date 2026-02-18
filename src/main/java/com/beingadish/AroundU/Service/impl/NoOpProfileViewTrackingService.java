package com.beingadish.AroundU.Service.impl;

import com.beingadish.AroundU.Service.ProfileViewTrackingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * No-op implementation of {@link ProfileViewTrackingService} used under the
 * {@code test} profile where Redis/Redisson is not available.
 */
@Service
@Profile("test")
@Slf4j
public class NoOpProfileViewTrackingService implements ProfileViewTrackingService {

    @Override
    public boolean trackView(Long viewerId, Long profileId) {
        log.debug("NoOp: skipping profile view tracking for viewer={}, profile={}", viewerId, profileId);
        return true;
    }

    @Override
    public long getViewCount(Long profileId) {
        log.debug("NoOp: returning 0 for profile view count, profile={}", profileId);
        return 0;
    }
}
