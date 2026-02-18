package com.beingadish.AroundU.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Records a failed Redis geo-index operation so it can be retried later.
 * <p>
 * When {@link com.beingadish.AroundU.Service.impl.JobServiceImpl} successfully
 * persists a job to PostgreSQL but the corresponding Redis geo-index write
 * fails, it saves a {@code FailedGeoSync} row. A scheduled retry job in
 * {@link com.beingadish.AroundU.Service.JobGeoSyncService} picks these up and
 * re-attempts the sync.
 */
@Entity
@Table(name = "failed_geo_syncs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedGeoSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The job ID whose geo-index operation failed.
     */
    @Column(nullable = false)
    private Long jobId;

    /**
     * The type of operation that failed: ADD, REMOVE, or UPDATE.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncOperation operation;

    /**
     * Latitude to sync (nullable for REMOVE operations).
     */
    @Column
    private Double latitude;

    /**
     * Longitude to sync (nullable for REMOVE operations).
     */
    @Column
    private Double longitude;

    /**
     * Number of retry attempts so far.
     */
    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    /**
     * Last error message from the failed attempt.
     */
    @Column(length = 1000)
    private String lastError;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Whether this sync has been resolved (successfully retried or manually
     * dismissed).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean resolved = false;

    public enum SyncOperation {
        ADD,
        REMOVE,
        UPDATE
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FailedGeoSync that = (FailedGeoSync) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
