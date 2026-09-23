package com.legalassist.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class PgVectorSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PgVectorSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public PgVectorSchemaInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            String dbProductName = connection.getMetaData().getDatabaseProductName();
            if (dbProductName != null && dbProductName.toLowerCase().contains("postgresql")) {
                log.info("PostgreSQL detected ({}); ensuring pgvector extension and column exists", dbProductName);
                jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector;");
                jdbcTemplate.execute("ALTER TABLE document_chunks ADD COLUMN IF NOT EXISTS embedding vector(768);");
                log.info("Successfully verified pgvector extension and document_chunks.embedding column");
            } else {
                log.info("Database is {} (non-PostgreSQL); skipping pgvector DDL initialization", dbProductName);
            }
        } catch (Exception e) {
            log.warn("Non-fatal: Could not verify pgvector schema extension/column DDL. Error: {}", e.getMessage());
        }
    }
}
