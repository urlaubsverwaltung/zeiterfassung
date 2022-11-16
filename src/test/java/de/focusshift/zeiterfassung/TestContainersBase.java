package de.focusshift.zeiterfassung;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.testcontainers.containers.PostgreSQLContainer.IMAGE;

public abstract class TestContainersBase {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(IMAGE + ":9.6")
        .withDatabaseName("zeiterfassung")
        .withInitScript("init-user-db.sql");

    @DynamicPropertySource
    static void postgresDBProperties(DynamicPropertyRegistry registry) {
        postgres.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.liquibase.parameters.database", postgres::getDatabaseName);
        registry.add("admin.datasource.username", postgres::getUsername);
        registry.add("admin.datasource.password", postgres::getPassword);
    }
}
