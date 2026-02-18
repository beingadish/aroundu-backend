package com.beingadish.AroundU.Utilities;

import com.beingadish.AroundU.Constants.Enums.SortDirection;
import com.beingadish.AroundU.Exceptions.InvalidSortFieldException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for the sorting infrastructure:
 * <ul>
 * <li>{@link SortValidator} – whitelist validation, SQL injection prevention,
 * multi-field sort</li>
 * <li>{@link DistanceUtils} – Haversine distance accuracy</li>
 * <li>{@link PopularityUtils} – popularity scoring</li>
 * <li>{@link SortDirection} – enum conversion</li>
 * </ul>
 */
class SortingTest {

    // ═══════════════════════════════════════════════════════════════
    //  SortValidator
    // ═══════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("SortValidator.validate()")
    class ValidateTests {

        @Test
        @DisplayName("should accept whitelisted job field (case-insensitive)")
        void acceptsWhitelistedField() {
            assertThatNoException().isThrownBy(()
                    -> SortValidator.validate("createdAt", SortDirection.DESC, SortValidator.JOB_FIELDS));
        }

        @Test
        @DisplayName("should accept field with different casing")
        void caseInsensitive() {
            assertThatNoException().isThrownBy(()
                    -> SortValidator.validate("CREATEDAT", SortDirection.ASC, SortValidator.JOB_FIELDS));
            assertThatNoException().isThrownBy(()
                    -> SortValidator.validate("EstimatedBudget", SortDirection.DESC, SortValidator.JOB_FIELDS));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t"})
        @DisplayName("should accept null or blank field (falls back to default)")
        void acceptsNullOrBlank(String field) {
            assertThatNoException().isThrownBy(()
                    -> SortValidator.validate(field, SortDirection.DESC, SortValidator.JOB_FIELDS));
        }

        @Test
        @DisplayName("should reject field not in whitelist")
        void rejectsUnknownField() {
            assertThatThrownBy(()
                    -> SortValidator.validate("nonExistentField", SortDirection.ASC, SortValidator.JOB_FIELDS))
                    .isInstanceOf(InvalidSortFieldException.class)
                    .hasMessageContaining("nonExistentField");
        }

        // ── SQL injection prevention ─────────────────────────────
        @ParameterizedTest
        @ValueSource(strings = {
            "createdAt; DROP TABLE jobs;--",
            "1 OR 1=1",
            "createdAt UNION SELECT * FROM users",
            "'); DELETE FROM jobs; --",
            "createdAt\"; DROP TABLE",
            "title' OR '1'='1"
        })
        @DisplayName("should reject SQL injection attempts")
        void rejectsSqlInjection(String maliciousField) {
            assertThatThrownBy(()
                    -> SortValidator.validate(maliciousField, SortDirection.ASC, SortValidator.JOB_FIELDS))
                    .isInstanceOf(InvalidSortFieldException.class);
        }

        @Test
        @DisplayName("should validate worker fields whitelist")
        void workerFieldsWhitelist() {
            assertThatNoException().isThrownBy(()
                    -> SortValidator.validate("rating", SortDirection.DESC, SortValidator.WORKER_FIELDS));
            assertThatNoException().isThrownBy(()
                    -> SortValidator.validate("experienceyears", SortDirection.ASC, SortValidator.WORKER_FIELDS));
        }

        @Test
        @DisplayName("should validate bid fields whitelist")
        void bidFieldsWhitelist() {
            assertThatNoException().isThrownBy(()
                    -> SortValidator.validate("amount", SortDirection.DESC, SortValidator.BID_FIELDS));
            assertThatNoException().isThrownBy(()
                    -> SortValidator.validate("bidamount", SortDirection.ASC, SortValidator.BID_FIELDS));
        }

        @Test
        @DisplayName("should reject job field when validating against worker whitelist")
        void crossEndpointRejection() {
            assertThatThrownBy(()
                    -> SortValidator.validate("estimatedbudget", SortDirection.ASC, SortValidator.WORKER_FIELDS))
                    .isInstanceOf(InvalidSortFieldException.class);
        }
    }

    @Nested
    @DisplayName("SortValidator.resolveField()")
    class ResolveFieldTests {

        @Test
        @DisplayName("should resolve aliased field to JPA property")
        void resolvesAlias() {
            String resolved = SortValidator.resolveField("estimatedbudget", SortValidator.JOB_FIELDS, "createdAt");
            assertThat(resolved).isEqualTo("price.amount");
        }

        @Test
        @DisplayName("should resolve rating alias for workers")
        void resolvesWorkerRating() {
            String resolved = SortValidator.resolveField("rating", SortValidator.WORKER_FIELDS, "createdAt");
            assertThat(resolved).isEqualTo("overallRating");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should return default when field is null or empty")
        void returnsDefaultForBlank(String field) {
            String resolved = SortValidator.resolveField(field, SortValidator.JOB_FIELDS, "createdAt");
            assertThat(resolved).isEqualTo("createdAt");
        }

        @Test
        @DisplayName("should return default for unknown field")
        void returnsDefaultForUnknown() {
            String resolved = SortValidator.resolveField("unknownField", SortValidator.JOB_FIELDS, "createdAt");
            assertThat(resolved).isEqualTo("createdAt");
        }
    }

    @Nested
    @DisplayName("SortValidator.isVirtualField()")
    class VirtualFieldTests {

        @ParameterizedTest
        @ValueSource(strings = {"distance", "Distance", "DISTANCE", "bidCount", "viewCount", "completedJobs", "successRate"})
        @DisplayName("should identify virtual fields")
        void identifiesVirtualFields(String field) {
            assertThat(SortValidator.isVirtualField(field)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"createdAt", "title", "price.amount", "overallRating"})
        @DisplayName("should identify non-virtual fields")
        void identifiesNonVirtualFields(String field) {
            assertThat(SortValidator.isVirtualField(field)).isFalse();
        }

        @Test
        @DisplayName("should return false for null")
        void falseForNull() {
            assertThat(SortValidator.isVirtualField(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("SortValidator.buildSort()")
    class BuildSortTests {

        @Test
        @DisplayName("should build Sort with resolved field and direction")
        void buildsSortCorrectly() {
            Sort sort = SortValidator.buildSort("estimatedbudget", SortDirection.ASC, SortValidator.JOB_FIELDS);
            assertThat(sort.isSorted()).isTrue();
            Sort.Order order = sort.iterator().next();
            assertThat(order.getProperty()).isEqualTo("price.amount");
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("should default to DESC when direction is null")
        void defaultsToDesc() {
            Sort sort = SortValidator.buildSort("createdAt", null, SortValidator.JOB_FIELDS);
            Sort.Order order = sort.iterator().next();
            assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("should return unsorted for virtual field")
        void unsortedForVirtualField() {
            Sort sort = SortValidator.buildSort("distance", SortDirection.ASC, SortValidator.JOB_FIELDS);
            assertThat(sort.isUnsorted()).isTrue();
        }

        @Test
        @DisplayName("should fall back to createdAt when field is null")
        void fallbackForNull() {
            Sort sort = SortValidator.buildSort(null, SortDirection.DESC, SortValidator.JOB_FIELDS);
            assertThat(sort.isSorted()).isTrue();
            Sort.Order order = sort.iterator().next();
            assertThat(order.getProperty()).isEqualTo("createdAt");
        }
    }

    @Nested
    @DisplayName("SortValidator.buildMultiSort()")
    class MultiSortTests {

        @Test
        @DisplayName("should combine primary and secondary sort fields")
        void combinesPrimaryAndSecondary() {
            Sort sort = SortValidator.buildMultiSort("title", SortDirection.ASC, "estimatedbudget", SortDirection.DESC, SortValidator.JOB_FIELDS);
            assertThat(sort.isSorted()).isTrue();

            var orders = sort.toList();
            assertThat(orders).hasSizeGreaterThanOrEqualTo(3); // primary + secondary + tertiary (createdAt)
            assertThat(orders.get(0).getProperty()).isEqualTo("title");
            assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
            assertThat(orders.get(1).getProperty()).isEqualTo("price.amount");
            assertThat(orders.get(1).getDirection()).isEqualTo(Sort.Direction.DESC);
            // tertiary: createdAt DESC for deterministic pagination
            assertThat(orders.get(2).getProperty()).isEqualTo("createdAt");
            assertThat(orders.get(2).getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("should work with primary only (no secondary)")
        void primaryOnly() {
            Sort sort = SortValidator.buildMultiSort("title", SortDirection.ASC, null, null, SortValidator.JOB_FIELDS);
            var orders = sort.toList();
            assertThat(orders).hasSizeGreaterThanOrEqualTo(2);
            assertThat(orders.get(0).getProperty()).isEqualTo("title");
        }

        @Test
        @DisplayName("should fall back to createdAt when virtual primary is used alone")
        void virtualPrimaryFallback() {
            Sort sort = SortValidator.buildMultiSort("distance", SortDirection.ASC, null, null, SortValidator.JOB_FIELDS);
            // distance → unsorted, so tertiary createdAt is the only sort
            var orders = sort.toList();
            assertThat(orders).isNotEmpty();
            assertThat(orders.get(0).getProperty()).isEqualTo("createdAt");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  DistanceUtils (Haversine)
    // ═══════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("DistanceUtils.haversine()")
    class DistanceTests {

        @Test
        @DisplayName("should return 0 for identical coordinates")
        void zeroForSamePoint() {
            double d = DistanceUtils.haversine(12.9716, 77.5946, 12.9716, 77.5946);
            assertThat(d).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should calculate Bangalore to Mumbai within 1% accuracy")
        void bangaloreToMumbai() {
            // Bangalore: 12.9716, 77.5946  Mumbai: 19.0760, 72.8777
            // Expected ~845 km
            double d = DistanceUtils.haversine(12.9716, 77.5946, 19.0760, 72.8777);
            assertThat(d).isBetween(835.0, 855.0);
        }

        @Test
        @DisplayName("should calculate New York to London within 1% accuracy")
        void newYorkToLondon() {
            // NYC: 40.7128, -74.0060  London: 51.5074, -0.1278
            // Expected ~5570 km
            double d = DistanceUtils.haversine(40.7128, -74.0060, 51.5074, -0.1278);
            assertThat(d).isBetween(5500.0, 5600.0);
        }

        @Test
        @DisplayName("should calculate short distance accurately")
        void shortDistance() {
            // Two points ~1.1 km apart in Bangalore
            double d = DistanceUtils.haversine(12.9716, 77.5946, 12.9810, 77.5946);
            assertThat(d).isBetween(0.9, 1.2);
        }

        @Test
        @DisplayName("should handle antipodal points (max distance ~20000 km)")
        void antipodalPoints() {
            double d = DistanceUtils.haversine(0.0, 0.0, 0.0, 180.0);
            assertThat(d).isBetween(20000.0, 20100.0);
        }

        @Test
        @DisplayName("should throw for null coordinates")
        void throwsForNullCoords() {
            assertThatThrownBy(() -> DistanceUtils.haversine(null, 77.0, 12.0, 77.0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> DistanceUtils.haversine(12.0, null, 12.0, 77.0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> DistanceUtils.haversine(12.0, 77.0, null, 77.0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> DistanceUtils.haversine(12.0, 77.0, 12.0, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  PopularityUtils
    // ═══════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("PopularityUtils")
    class PopularityTests {

        @Test
        @DisplayName("should calculate job popularity score with bids weighted higher")
        void jobPopularity() {
            int score = PopularityUtils.calculateJobPopularityScore(5, 10);
            // 5*3 + 10*1 = 25
            assertThat(score).isEqualTo(25);
        }

        @Test
        @DisplayName("should return 0 for null counts")
        void jobPopularityNullCounts() {
            int score = PopularityUtils.calculateJobPopularityScore(null, null);
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle zero bids and views")
        void jobPopularityZero() {
            int score = PopularityUtils.calculateJobPopularityScore(0, 0);
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("should calculate worker score with component weights")
        void workerScore() {
            // rating=5.0 → (5/5)*100*0.4 = 40, successRate=80 → 80*0.4=32, completed=50 → min(50,100)*0.2=10
            double score = PopularityUtils.calculateWorkerScore(80.0, 5.0, 50);
            assertThat(score).isCloseTo(82.0, within(0.1));
        }

        @Test
        @DisplayName("should return 0 for all null worker params")
        void workerScoreAllNull() {
            double score = PopularityUtils.calculateWorkerScore(null, null, null);
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should cap volume component at 100 completed jobs")
        void workerScoreVolumeCapped() {
            double withCap = PopularityUtils.calculateWorkerScore(0.0, 0.0, 500);
            double atCap = PopularityUtils.calculateWorkerScore(0.0, 0.0, 100);
            assertThat(withCap).isEqualTo(atCap);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SortDirection enum
    // ═══════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("SortDirection")
    class SortDirectionTests {

        @Test
        @DisplayName("should convert ASC to Spring Direction.ASC")
        void ascConversion() {
            assertThat(SortDirection.ASC.toSpringDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("should convert DESC to Spring Direction.DESC")
        void descConversion() {
            assertThat(SortDirection.DESC.toSpringDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("should parse 'asc' case-insensitively")
        void parseAsc() {
            assertThat(SortDirection.fromString("asc")).isEqualTo(SortDirection.ASC);
            assertThat(SortDirection.fromString("ASC")).isEqualTo(SortDirection.ASC);
            assertThat(SortDirection.fromString("Asc")).isEqualTo(SortDirection.ASC);
        }

        @Test
        @DisplayName("should default to DESC for null/blank/invalid input")
        void defaultToDesc() {
            assertThat(SortDirection.fromString(null)).isEqualTo(SortDirection.DESC);
            assertThat(SortDirection.fromString("")).isEqualTo(SortDirection.DESC);
            assertThat(SortDirection.fromString("   ")).isEqualTo(SortDirection.DESC);
            assertThat(SortDirection.fromString("INVALID")).isEqualTo(SortDirection.DESC);
        }
    }
}
