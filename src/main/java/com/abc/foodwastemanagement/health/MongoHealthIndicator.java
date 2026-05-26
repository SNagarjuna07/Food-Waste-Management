package com.abc.foodwastemanagement.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class MongoHealthIndicator implements HealthIndicator {

    private final MongoTemplate mongoTemplate;

    public Health health() {
        try {
            mongoTemplate.executeCommand("{ ping: 1 }");

            return Health.up()
                    .withDetail("database", mongoTemplate.getDb().getName())
                    .withDetail("check", "custom-mongo")
                    .build();

        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
