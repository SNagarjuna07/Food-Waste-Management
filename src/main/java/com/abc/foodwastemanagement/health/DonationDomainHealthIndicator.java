package com.abc.foodwastemanagement.health;

import com.abc.foodwastemanagement.repository.CollectionCenterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DonationDomainHealthIndicator implements HealthIndicator {

    private final CollectionCenterRepository collectionCenterRepository;

    @Override
    public Health health() {
        try {
            long activeCenters = collectionCenterRepository.countByActiveTrue();

            if (activeCenters == 0) {
                return Health.down()
                        .withDetail("reason", "No active collection centers")
                        .withDetail("activeCenters", 0)
                        .build();
            }

            return Health.up()
                    .withDetail("activeCenters", activeCenters)
                    .withDetail("domain", "Collection center")
                    .build();

        } catch (Exception ex) {
            return Health.down()
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
