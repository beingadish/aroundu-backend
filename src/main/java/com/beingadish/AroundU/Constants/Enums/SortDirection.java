package com.beingadish.AroundU.Constants.Enums;

/**
 * Sort direction for paginated list endpoints.
 */
public enum SortDirection {
    ASC,
    DESC;

    /**
     * Converts this enum to the Spring Data
     * {@link org.springframework.data.domain.Sort.Direction}.
     */
    public org.springframework.data.domain.Sort.Direction toSpringDirection() {
        return this == ASC
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;
    }

    /**
     * Case-insensitive parse with a safe default of {@code DESC}.
     */
    public static SortDirection fromString(String value) {
        if (value == null || value.isBlank()) {
            return DESC;
        }
        try {
            return SortDirection.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DESC;
        }
    }
}
