package com.beingadish.AroundU.location.service;

import java.util.List;
import java.util.Set;

public interface JobGeoService {

    void addOrUpdateOpenJob(Long jobId, Double latitude, Double longitude);

    void removeOpenJob(Long jobId);

    List<Long> findNearbyOpenJobs(Double latitude, Double longitude, double radiusKm, int limit);

    /**
     * Return all member identifiers stored in the geo index.
     */
    Set<String> getAllGeoMembers();
}
