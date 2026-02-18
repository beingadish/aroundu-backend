package com.beingadish.AroundU.Service;

import java.util.List;

public interface JobGeoService {

    void addOrUpdateOpenJob(Long jobId, Double latitude, Double longitude);

    void removeOpenJob(Long jobId);

    List<Long> findNearbyOpenJobs(Double latitude, Double longitude, double radiusKm, int limit);
}
