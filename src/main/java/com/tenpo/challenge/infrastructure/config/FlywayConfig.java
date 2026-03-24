package com.tenpo.challenge.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway is configured manually here because Spring Boot's auto-configuration
 * only kicks in when a javax.sql.DataSource bean is present.
 *
 * With R2DBC there is no DataSource, so we supply the JDBC URL directly.
 * The JDBC PostgreSQL driver (org.postgresql:postgresql) is on the classpath
 * for this sole purpose; it is never used at runtime for queries.
 */
@Slf4j
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(
            @Value("${spring.flyway.url}") String url,
            @Value("${spring.flyway.username}") String username,
            @Value("${spring.flyway.password}") String password) {

        log.info("Running Flyway database migrations against {}", url);

        return Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .load();
    }
}
