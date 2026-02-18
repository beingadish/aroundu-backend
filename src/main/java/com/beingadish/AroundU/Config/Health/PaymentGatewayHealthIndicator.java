package com.beingadish.AroundU.Config.Health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for the external payment gateway integration.
 * Placeholder: replace the check with an actual gateway connectivity test
 * (e.g., a lightweight HEAD or status call to the payment provider).
 */
@Component
public class PaymentGatewayHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // TODO: replace with real gateway health-check call
            boolean gatewayReachable = checkGateway();
            if (gatewayReachable) {
                return Health.up()
                        .withDetail("paymentGateway", "Reachable")
                        .build();
            }
            return Health.down()
                    .withDetail("paymentGateway", "Unreachable")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("paymentGateway", "Error")
                    .withException(e)
                    .build();
        }
    }

    private boolean checkGateway() {
        // Stub: always returns true until real gateway integration is in place
        return true;
    }
}
