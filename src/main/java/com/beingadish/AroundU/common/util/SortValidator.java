package com.beingadish.AroundU.common.util;

import com.beingadish.AroundU.common.constants.enums.SortDirection;
import com.beingadish.AroundU.common.exception.InvalidSortFieldException;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Validates and builds Spring {@link Sort} objects from user-supplied
 * parameters.
 * <p>
 * Every sortable field is whitelisted per endpoint so that arbitrary column
 * names (SQL injection vectors) can never reach the query layer.
 * <p>
 * Field names are matched <b>case-insensitively</b>; the canonical
 * (entity-level) property name is always used in the resulting {@link Sort}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * SortValidator.validate("estimatedBudget", SortDirection.DESC, SortValidator.JOB_FIELDS);
 * Sort sort = SortValidator.buildSort("estimatedBudget", SortDirection.DESC, SortValidator.JOB_FIELDS);
 * }</pre>
 */
public final class SortValidator {

    private SortValidator() {
        // utility class
    }

    // ──────────────────────────────────────────────────────────────
    // Whitelists — key = lower-case alias, value = JPA property name
    // ──────────────────────────────────────────────────────────────
    /**
     * Jobs endpoint allowed sort fields. Maps user-facing (lower-case) name →
     * JPA entity property.
     */
    public static final Map<String, String> JOB_FIELDS;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("createdat", "createdAt");
        m.put("updatedat", "updatedAt");
        m.put("estimatedbudget", "price.amount");
        m.put("title", "title");
        m.put("jobstatus", "jobStatus");
        m.put("scheduledstarttime", "createdAt"); // alias – maps to createdAt until a dedicated column exists
        m.put("distance", "distance");             // handled in application-level sorting
        m.put("bidcount", "bidCount");             // virtual – popularity sorting
        m.put("viewcount", "viewCount");           // virtual – popularity sorting
        JOB_FIELDS = Collections.unmodifiableMap(m);
    }

    /**
     * Workers endpoint allowed sort fields.
     */
    public static final Map<String, String> WORKER_FIELDS;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("createdat", "createdAt");
        m.put("rating", "overallRating");
        m.put("overallrating", "overallRating");
        m.put("completedjobs", "completedJobs"); // virtual
        m.put("successrate", "successRate");     // virtual
        m.put("experienceyears", "experienceYears");
        WORKER_FIELDS = Collections.unmodifiableMap(m);
    }

    /**
     * Bids endpoint allowed sort fields.
     */
    public static final Map<String, String> BID_FIELDS;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("createdat", "createdAt");
        m.put("amount", "bidAmount");
        m.put("bidamount", "bidAmount");
        m.put("estimatedduration", "estimatedDuration"); // for future use
        BID_FIELDS = Collections.unmodifiableMap(m);
    }

    // Virtual fields that cannot be pushed to the DB via Sort.by()
    private static final Set<String> VIRTUAL_FIELDS = Set.of(
            "distance", "bidCount", "viewCount", "completedJobs", "successRate"
    );

    // ──────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────
    /**
     * Validates that {@code field} is present in the given whitelist.
     *
     * @param field the user-supplied sort field (case-insensitive)
     * @param direction the sort direction
     * @param allowedFields whitelist map
     * @throws InvalidSortFieldException if the field is not whitelisted
     */
    public static void validate(String field, SortDirection direction, Map<String, String> allowedFields) {
        if (field == null || field.isBlank()) {
            return; // will fall back to default
        }
        String key = field.trim().toLowerCase();
        if (!allowedFields.containsKey(key)) {
            throw new InvalidSortFieldException("Invalid sort field '%s'. Allowed: %s"
                    .formatted(field, allowedFields.values()));
        }
    }

    /**
     * Resolves the canonical JPA property name for a user-supplied field.
     *
     * @return the JPA property name, or {@code defaultField} when input is
     * blank
     */
    public static String resolveField(String field, Map<String, String> allowedFields, String defaultField) {
        if (field == null || field.isBlank()) {
            return defaultField;
        }
        String key = field.trim().toLowerCase();
        return allowedFields.getOrDefault(key, defaultField);
    }

    /**
     * Returns {@code true} if the resolved field requires application-level
     * sorting (e.g. distance, bid count) rather than a DB ORDER BY.
     */
    public static boolean isVirtualField(String field) {
        if (field == null) {
            return false;
        }
        String resolved = field.trim().toLowerCase();
        // Check both raw aliases and resolved property names
        return VIRTUAL_FIELDS.stream()
                .anyMatch(v -> v.equalsIgnoreCase(resolved) || v.equalsIgnoreCase(field));
    }

    /**
     * Builds a Spring {@link Sort} from validated parameters.
     * <p>
     * Returns {@link Sort#unsorted()} for virtual fields (those are sorted
     * in-memory).
     */
    public static Sort buildSort(String field, SortDirection direction,
            Map<String, String> allowedFields) {
        String resolved = resolveField(field, allowedFields, "createdAt");
        SortDirection dir = direction != null ? direction : SortDirection.DESC;

        if (isVirtualField(resolved)) {
            return Sort.unsorted();
        }
        return Sort.by(dir.toSpringDirection(), resolved);
    }

    /**
     * Builds a multi-field {@link Sort}.
     *
     * @param primaryField primary sort field
     * @param primaryDir primary direction
     * @param secondaryField optional secondary sort field (may be null)
     * @param secondaryDir optional secondary direction
     * @param allowedFields whitelist
     * @return combined {@link Sort}, falling back to createdAt DESC as tertiary
     */
    public static Sort buildMultiSort(String primaryField, SortDirection primaryDir,
            String secondaryField, SortDirection secondaryDir,
            Map<String, String> allowedFields) {
        Sort primary = buildSort(primaryField, primaryDir, allowedFields);

        if (secondaryField != null && !secondaryField.isBlank()) {
            validate(secondaryField, secondaryDir, allowedFields);
            Sort secondary = buildSort(secondaryField, secondaryDir, allowedFields);
            if (secondary.isSorted()) {
                primary = primary.isSorted() ? primary.and(secondary) : secondary;
            }
        }

        // Tertiary: always append createdAt DESC for deterministic pagination
        Sort tertiary = Sort.by(Sort.Direction.DESC, "createdAt");
        return primary.isSorted() ? primary.and(tertiary) : tertiary;
    }
}
