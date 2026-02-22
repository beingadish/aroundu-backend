package com.beingadish.AroundU.job.dto;

import com.beingadish.AroundU.common.constants.enums.JobStatus;
import com.beingadish.AroundU.common.constants.enums.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class JobFilterRequest {

    private List<JobStatus> statuses;
    private LocalDate startDate;
    private LocalDate endDate;

    @Min(0)
    private Integer page = 0;

    @Positive
    @Max(100)
    private Integer size = 20;

    // ── Sorting ──────────────────────────────────────────────────
    /**
     * Primary sort field (default: createdAt). Validated against whitelist.
     */
    private String sortBy = "createdAt";

    /**
     * Primary sort direction (default: DESC).
     */
    private SortDirection sortDirection = SortDirection.DESC;

    /**
     * Optional secondary sort field for multi-field sorting.
     */
    private String secondarySortBy;

    /**
     * Secondary sort direction (default: ASC).
     */
    private SortDirection secondarySortDirection = SortDirection.ASC;

    // ── Distance sorting ─────────────────────────────────────────
    /**
     * When true, results are sorted by proximity to the given coordinates.
     */
    private Boolean sortByDistance = false;

    /**
     * Latitude of the reference point for distance sorting.
     */
    private Double distanceLatitude;

    /**
     * Longitude of the reference point for distance sorting.
     */
    private Double distanceLongitude;

    /**
     * Produces a deterministic, stable cache key based on all filter fields.
     * Must NOT rely on {@link Object#hashCode()}.
     */
    public String toCacheKey() {
        return page + ":" + size
                + ":" + sortBy + ":" + sortDirection
                + ":" + secondarySortBy + ":" + secondarySortDirection
                + ":" + statuses
                + ":" + startDate + ":" + endDate
                + ":" + sortByDistance + ":" + distanceLatitude + ":" + distanceLongitude;
    }
}
