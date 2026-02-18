package com.beingadish.AroundU.location.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoLocation;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import com.beingadish.AroundU.location.service.JobGeoService;

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

            // Sort ascending (nearest first) and limit at the Redis level so we
            // always get the closest N jobs, not an arbitrary subset.
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                    .newGeoRadiusArgs()
                    .sortAscending()
                    .limit(Math.max(limit, 1));

            var results = ops.radius(OPEN_JOBS_GEO_KEY, within, args);
            if (results == null || results.getContent().isEmpty()) {
                return Collections.emptyList();
            }
            return results.getContent()
                    .stream()
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

    @Override
    public Set<String> getAllGeoMembers() {
        try {
            Set<String> members = stringRedisTemplate.opsForZSet().range(OPEN_JOBS_GEO_KEY, 0, -1);
            return members != null ? members : Collections.emptySet();
        } catch (Exception ex) {
            log.warn("Failed to retrieve all geo members: {}", ex.getMessage());
            return Collections.emptySet();
        }
    }
}
