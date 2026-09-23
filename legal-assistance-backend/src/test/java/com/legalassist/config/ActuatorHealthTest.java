package com.legalassist.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorHealthTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EndpointAutoConfiguration.class,
                    HealthContributorRegistryAutoConfiguration.class,
                    HealthContributorAutoConfiguration.class,
                    HealthEndpointAutoConfiguration.class
            ))
            .withPropertyValues(
                    "management.endpoints.web.exposure.include=health",
                    "management.endpoint.health.show-details=never",
                    "management.health.db.enabled=false"
            );

    @Test
    void healthEndpointShouldBeConfiguredAndReturnUpStatus() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HealthEndpoint.class);
            HealthEndpoint healthEndpoint = context.getBean(HealthEndpoint.class);
            assertThat(healthEndpoint.health().getStatus()).isEqualTo(Status.UP);
        });
    }
}
