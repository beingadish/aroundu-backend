package com.beingadish.AroundU.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalised cron expressions and tunables for scheduled tasks. Defaults
 * match production; per-profile overrides live in
 * {@code application-{profile}.yml}.
 */
@Configuration
@ConfigurationProperties(prefix = "scheduler")
@Data
public class SchedulerProperties {

    // ── Cron expressions ─────────────────────────────────────────────────
    private String userCleanupCron = "0 0 2 * * ?";
    private String jobExpirationCron = "0 0 * * * ?";
    private String reminderCron = "0 0 */6 * * ?";
    private String cacheSyncCron = "0 */30 * * * ?";
    private String analyticsCron = "0 0 3 * * ?";

    // ── Tunables ─────────────────────────────────────────────────────────
    /**
     * Inactive threshold for user cleanup (years).
     */
    private int userInactiveYears = 2;

    /**
     * Maximum age in days for open jobs before they are considered expired.
     */
    private int jobExpirationDays = 30;

    /**
     * Hours after which a zero-bid job triggers a reminder email.
     */
    private int reminderThresholdHours = 24;

    /**
     * Whether scheduled tasks are enabled (master switch).
     */
    private boolean enabled = true;
}
