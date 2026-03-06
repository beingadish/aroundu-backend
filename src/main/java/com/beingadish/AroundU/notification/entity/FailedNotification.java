package com.beingadish.AroundU.notification.entity;

import jakarta.persistence.*;
import lombok.*;

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
