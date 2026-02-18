package com.beingadish.AroundU.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stores daily aggregated metrics. One row per day, computed by the
 * {@code AnalyticsScheduler} at 3 AM.
 */
@Entity
@Table(name = "aggregated_metrics", uniqueConstraints = {
    @UniqueConstraint(columnNames = "metricDate")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AggregatedMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate metricDate;

    // ── Counts ───────────────────────────────────────────────────────────
    @Builder.Default
    private Long jobsCreated = 0L;

    @Builder.Default
    private Long bidsPlaced = 0L;

    @Builder.Default
    private Long jobsCompleted = 0L;

    @Builder.Default
    private Double revenueTotal = 0.0;

    @Builder.Default
    private Double averageBidPerJob = 0.0;

    // ── Trends ───────────────────────────────────────────────────────────
    @Builder.Default
    private Double weekOverWeekGrowth = 0.0;

    @Builder.Default
    private Double monthOverMonthGrowth = 0.0;

    // ── Insights ─────────────────────────────────────────────────────────
    /**
     * Comma-separated trending category names.
     */
    @Column(length = 1000)
    private String trendingCategories;

    /**
     * Comma-separated top worker IDs from the period.
     */
    @Column(length = 500)
    private String topWorkerIds;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime computedAt;
}
