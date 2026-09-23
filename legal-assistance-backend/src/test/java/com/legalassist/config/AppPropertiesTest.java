package com.legalassist.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AppPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Configuration
    @EnableConfigurationProperties(AppProperties.class)
    static class TestConfig {
    }

    @Test
    void shouldBindDefaultEnvironmentWhenPropertyNotProvided() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AppProperties.class);
            AppProperties properties = context.getBean(AppProperties.class);
            assertThat(properties.environment()).isEqualTo("development");
        });
    }

    @Test
    void shouldBindCustomEnvironmentFromProperty() {
        contextRunner
                .withPropertyValues("app.environment=staging")
                .run(context -> {
                    assertThat(context).hasSingleBean(AppProperties.class);
                    AppProperties properties = context.getBean(AppProperties.class);
                    assertThat(properties.environment()).isEqualTo("staging");
                });
    }
}
