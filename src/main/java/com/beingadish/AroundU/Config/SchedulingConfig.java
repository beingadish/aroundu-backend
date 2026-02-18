package com.beingadish.AroundU.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@link org.springframework.scheduling.annotation.Scheduled} support
 * for non-test profiles. Tests use direct invocation instead.
 */
@Configuration
@EnableScheduling
@Profile("!test")
public class SchedulingConfig {
}
