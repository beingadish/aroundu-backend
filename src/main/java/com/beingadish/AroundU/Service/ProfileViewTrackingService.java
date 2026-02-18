package com.beingadish.AroundU.Service;

/**
 * Tracks profile views using a Bloom filter to avoid counting repeated views
 * by the same user within the same hour.
 * <p>
 * The key format includes the current hour bucket so that views naturally
 * "expire" each hour when a new bucket is used.
 */
public interface ProfileViewTrackingService {

    /**
     * Track a profile view event.
     *
     * @param viewerId  the user viewing the profile
     * @param profileId the profile being viewed
     * @return {@code true} if this is a new view (counter incremented),
     *         {@code false} if the view was a repeat within the same hour
     */
    boolean trackView(Long viewerId, Long profileId);

    /**
     * Get the total view count for a profile.
     *
     * @param profileId the profile id
     * @return the total view count
     */
    long getViewCount(Long profileId);
}
