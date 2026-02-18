package com.beingadish.AroundU.Config.Health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Custom health indicator that verifies the database connection pool is alive
 * by executing {@code connection.isValid(2)}.
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return Health.up()
                        .withDetail("database", "Available")
                        .withDetail("product", connection.getMetaData().getDatabaseProductName())
                        .withDetail("url", connection.getMetaData().getURL())
                        .build();
            }
            return Health.down()
                    .withDetail("database", "Connection invalid")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "Unavailable")
                    .withException(e)
                    .build();
        }
    }
}
