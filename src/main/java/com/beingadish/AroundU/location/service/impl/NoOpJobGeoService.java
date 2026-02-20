package com.beingadish.AroundU.location.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import com.beingadish.AroundU.location.service.JobGeoService;

@Service
@Profile("railway")
@Slf4j
public class NoOpJobGeoService implements JobGeoService {

    @Override
    public void addOrUpdateOpenJob(Long jobId, Double latitude, Double longitude) {
        log.debug("Railway profile: skipping geo add for jobId={} (Redis disabled)", jobId);
    }

    @Override
    public void removeOpenJob(Long jobId) {
        log.debug("Railway profile: skipping geo remove for jobId={} (Redis disabled)", jobId);
    }

    @Override
    public List<Long> findNearbyOpenJobs(Double latitude, Double longitude, double radiusKm, int limit) {
        log.debug("Railway profile: geo search bypassed (Redis disabled)");
        return Collections.emptyList();
    }

    @Override
    public Set<String> getAllGeoMembers() {
        return Collections.emptySet();
    }
}
