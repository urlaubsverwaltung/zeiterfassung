package de.focusshift.zeiterfassung;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public class TestPostgreSQLContainer extends PostgreSQLContainer<TestPostgreSQLContainer> {

    private static final String VERSION = "16.1";

    public TestPostgreSQLContainer() {
        super(IMAGE + ":" + VERSION);
        this.withDatabaseName("zeiterfassung");
        this.withCommand("--max_connections=1000", "--shared_buffers=240MB");
        this.withInitScript("init-user-db.sql");
    }

    /**
     * Sets the spring datasource configuration properties.
     *
     * <p>Usage:</p>
     * <pre><code>
     * static final TestPostgreContainer postgre = new TestPostgreContainer();
     * &#64;DynamicPropertySource
     * static void setupDataSource(DynamicPropertySource registry) {
     *     postgre.start();
     *     postgre.configureSpringDataSource(registry);
     * }
     * </code>
     * </pre>
     *
     * @param registry
     */
    public void configureSpringDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", this::getJdbcUrl);
        registry.add("spring.liquibase.parameters.database", this::getDatabaseName);
        registry.add("admin.datasource.username", this::getUsername);
        registry.add("admin.datasource.password", this::getPassword);
    }
}
