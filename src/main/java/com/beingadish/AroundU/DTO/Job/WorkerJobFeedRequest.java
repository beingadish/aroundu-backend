package com.beingadish.AroundU.DTO.Job;

import com.beingadish.AroundU.Constants.Enums.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class WorkerJobFeedRequest {

    private List<Long> skillIds;
    private Double radiusKm;
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
     * When true, results are sorted by proximity to the worker's location.
     */
    private Boolean sortByDistance = false;
}
