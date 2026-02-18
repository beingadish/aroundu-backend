package com.beingadish.AroundU.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.GeoResult;
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
@Profile("!railway")
public class RedisJobGeoService implements JobGeoService {

    private static final String OPEN_JOBS_GEO_KEY = "geo:jobs:open";
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void addOrUpdateOpenJob(Long jobId, Double latitude, Double longitude) {
        if (jobId == null || latitude == null || longitude == null) {
            log.debug("Skipping geo add, missing coordinates for jobId={} lat={} lon={}.", jobId, latitude, longitude);
            return;
        }
        try {
            GeoOperations<String, String> ops = stringRedisTemplate.opsForGeo();
            ops.add(OPEN_JOBS_GEO_KEY, new Point(longitude, latitude), jobId.toString());
            log.debug("Added jobId={} to geo set with lat={} lon={}", jobId, latitude, longitude);
        } catch (Exception ex) {
            log.warn("Redis geo add failed for jobId={}: {}", jobId, ex.getMessage());
        }
    }

    @Override
    public void removeOpenJob(Long jobId) {
        if (jobId == null) {
            return;
        }
        try {
            GeoOperations<String, String> ops = stringRedisTemplate.opsForGeo();
            ops.remove(OPEN_JOBS_GEO_KEY, jobId.toString());
            log.debug("Removed jobId={} from geo set", jobId);
        } catch (Exception ex) {
            log.warn("Redis geo remove failed for jobId={}: {}", jobId, ex.getMessage());
        }
    }

    @Override
    public List<Long> findNearbyOpenJobs(Double latitude, Double longitude, double radiusKm, int limit) {
        if (latitude == null || longitude == null) {
            log.debug("Geo search skipped, missing worker coordinates lat={} lon={}", latitude, longitude);
            return Collections.emptyList();
        }
        try {
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
        } catch (Exception ex) {
            log.warn("Redis geo search failed for lat={} lon={}: {}", latitude, longitude, ex.getMessage());
            return Collections.emptyList();
        }
    }
}
