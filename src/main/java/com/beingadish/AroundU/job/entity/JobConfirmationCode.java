package com.beingadish.AroundU.job.entity;

import com.beingadish.AroundU.common.constants.enums.JobCodeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Stores one-time confirmation codes (OTPs) for job start and release
 * verification.
 * <p>
 * Each code is a 6-digit secure-random number with a configurable expiry
 * window. Only the latest OTP is valid; regeneration invalidates old codes.
 * Attempt counts are tracked to prevent brute-force attacks.
 */
@Entity
@Table(name = "job_confirmation_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobConfirmationCode {

    /**
     * Maximum attempts before codes are locked.
     */
    public static final int MAX_ATTEMPTS = 5;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    @JoinColumn(name = "job_id", unique = true)
    private Job job;
    @Column(length = 6, nullable = false)
    private String startCode;
    @Column(length = 6, nullable = false)
    private String releaseCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobCodeStatus status = JobCodeStatus.START_PENDING;
    /**
     * When the current start code was generated.
     */
    @Column(nullable = false)
    private LocalDateTime startCodeGeneratedAt;
    /**
     * When the current start code expires.
     */
    @Column(nullable = false)
    private LocalDateTime startCodeExpiresAt;
    /**
     * When the current release code was generated.
     */
    @Column(nullable = false)
    private LocalDateTime releaseCodeGeneratedAt;
    /**
     * When the current release code expires.
     */
    @Column(nullable = false)
    private LocalDateTime releaseCodeExpiresAt;
    /**
     * Number of failed start code verification attempts.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer startCodeAttempts = 0;
    /**
     * Number of failed release code verification attempts.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer releaseCodeAttempts = 0;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Returns true if the start code has expired.
     */
    public boolean isStartCodeExpired() {
        return LocalDateTime.now().isAfter(startCodeExpiresAt);
    }

    /**
     * Returns true if the release code has expired.
     */
    public boolean isReleaseCodeExpired() {
        return LocalDateTime.now().isAfter(releaseCodeExpiresAt);
    }

    /**
     * Returns true if start code attempts are exhausted.
     */
    public boolean isStartCodeLocked() {
        return startCodeAttempts >= MAX_ATTEMPTS;
    }

    /**
     * Returns true if release code attempts are exhausted.
     */
    public boolean isReleaseCodeLocked() {
        return releaseCodeAttempts >= MAX_ATTEMPTS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JobConfirmationCode that = (JobConfirmationCode) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
