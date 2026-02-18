package com.beingadish.AroundU.Entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Persists failed notification attempts for manual or scheduled retry. When an
 * async notification (email, push, SMS) fails, a record is created here so the
 * system can retry later without losing the notification.
 */
@Entity
@Table(name = "failed_notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long jobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false)
    private String recipient;

    @Column(length = 2000)
    private String errorMessage;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private boolean resolved;

    @Builder.Default
    @Column(nullable = false)
    private Instant failedAt = Instant.now();

    private Instant resolvedAt;

    public enum NotificationType {
        EMAIL, PUSH, SMS
    }
}
