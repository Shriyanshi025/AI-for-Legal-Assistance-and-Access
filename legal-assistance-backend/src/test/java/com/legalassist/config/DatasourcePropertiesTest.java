package com.legalassist.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DatasourcePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class));

    @Test
    void shouldBindDatasourceConfigurationProperties() {
        contextRunner
                .withPropertyValues(
                        "spring.datasource.url=jdbc:postgresql://db.example.supabase.co:5432/postgres?sslmode=require",
                        "spring.datasource.username=postgres_user",
                        "spring.datasource.password=test_secret",
                        "spring.datasource.driver-class-name=org.postgresql.Driver"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSourceProperties.class);
                    DataSourceProperties properties = context.getBean(DataSourceProperties.class);
                    assertThat(properties.getUrl()).isEqualTo("jdbc:postgresql://db.example.supabase.co:5432/postgres?sslmode=require");
                    assertThat(properties.getUsername()).isEqualTo("postgres_user");
                    assertThat(properties.getDriverClassName()).isEqualTo("org.postgresql.Driver");
                });
    }

    @Test
    void shouldResolveDatasourceFromEnvironmentPlaceholders() {
        contextRunner
                .withPropertyValues(
                        "SPRING_DATASOURCE_URL=jdbc:postgresql://db.example.supabase.co:5432/postgres?sslmode=require",
                        "SPRING_DATASOURCE_USERNAME=postgres_user",
                        "SPRING_DATASOURCE_PASSWORD=test_secret",
                        "spring.datasource.url=${SPRING_DATASOURCE_URL:}",
                        "spring.datasource.username=${SPRING_DATASOURCE_USERNAME:}",
                        "spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}",
                        "spring.datasource.driver-class-name=org.postgresql.Driver"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSourceProperties.class);
                    DataSourceProperties properties = context.getBean(DataSourceProperties.class);
                    assertThat(properties.getUrl()).isEqualTo("jdbc:postgresql://db.example.supabase.co:5432/postgres?sslmode=require");
                    assertThat(properties.getUsername()).isEqualTo("postgres_user");
                    assertThat(properties.getPassword()).isNotEmpty();
                });
    }
}
