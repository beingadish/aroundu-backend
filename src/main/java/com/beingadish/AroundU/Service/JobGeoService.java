package com.beingadish.AroundU.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoLocation;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobGeoService {

    private static final String OPEN_JOBS_GEO_KEY = "geo:jobs:open";
    private final StringRedisTemplate stringRedisTemplate;

    public void addOrUpdateOpenJob(Long jobId, Double latitude, Double longitude) {
        if (jobId == null || latitude == null || longitude == null) {
            log.debug("Skipping geo add, missing coordinates for jobId={} lat={} lon={}.", jobId, latitude, longitude);
            return;
        }
        GeoOperations<String, String> ops = stringRedisTemplate.opsForGeo();
        ops.add(OPEN_JOBS_GEO_KEY, new Point(longitude, latitude), jobId.toString());
        log.debug("Added jobId={} to geo set with lat={} lon={}", jobId, latitude, longitude);
    }

    public void removeOpenJob(Long jobId) {
        if (jobId == null) {
            return;
        }
        GeoOperations<String, String> ops = stringRedisTemplate.opsForGeo();
        ops.remove(OPEN_JOBS_GEO_KEY, jobId.toString());
        log.debug("Removed jobId={} from geo set", jobId);
    }

    public List<Long> findNearbyOpenJobs(Double latitude, Double longitude, double radiusKm, int limit) {
        if (latitude == null || longitude == null) {
            log.debug("Geo search skipped, missing worker coordinates lat={} lon={}", latitude, longitude);
            return Collections.emptyList();
        }
        GeoOperations<String, String> ops = stringRedisTemplate.opsForGeo();
        Circle within = new Circle(new Point(longitude, latitude), new Distance(radiusKm, Metrics.KILOMETERS));
        var results = ops.radius(OPEN_JOBS_GEO_KEY, within);
        if (results == null || results.getContent().isEmpty()) {
            return Collections.emptyList();
        }
        return results.getContent()
                .stream()
                .limit(Math.max(limit, 0))
                .map(GeoResult::getContent)
                .map(GeoLocation::getName)
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}
