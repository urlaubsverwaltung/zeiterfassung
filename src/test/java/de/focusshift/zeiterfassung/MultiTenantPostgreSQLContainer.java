package de.focusshift.zeiterfassung;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Extend this class for zeiterfassung single tenant based integration tests.
 */
public class MultiTenantPostgreSQLContainer extends PostgreSQLContainer {

    private static final String VERSION = "17.7";

    public MultiTenantPostgreSQLContainer() {
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
     * static final MultiTenantPostgreSQLContainer postgres = new MultiTenantPostgreSQLContainer();
     * &#64;DynamicPropertySource
     * static void setupDataSource(DynamicPropertySource registry) {
     *     postgres.start();
     *     postgres.configureSpringDataSource(registry);
     * }
     * </code>
     * </pre>
     *
     * @param registry
     */
    public void configureSpringDataSource(DynamicPropertyRegistry registry) {
        // unprivileged user with active row-level-security
        registry.add("spring.datasource.url", this::getJdbcUrl);
        // ensure to use same values that are defined in init-user-db.sql!
        registry.add("spring.datasource.username", () -> "app_user");
        registry.add("spring.datasource.password", () -> "app_password");

        // privileged user from postgres
        registry.add("admin.datasource.url", this::getJdbcUrl);
        registry.add("admin.datasource.username", this::getUsername);
        registry.add("admin.datasource.password", this::getPassword);

        // ensure liquibase is using the privileged user from postgres
        registry.add("spring.liquibase.url", this::getJdbcUrl);
        registry.add("spring.liquibase.user", this::getUsername);
        registry.add("spring.liquibase.password", this::getPassword);
    }
}
