package de.focusshift.zeiterfassung;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

/**
 * Extend this class for zeiterfassung single tenant based integration tests.
 */
public class SingleTenantPostgreSQLContainer extends PostgreSQLContainer<SingleTenantPostgreSQLContainer> {

    private static final String VERSION = "16.1";

    public SingleTenantPostgreSQLContainer() {
        super(IMAGE + ":" + VERSION);
        this.withDatabaseName("zeiterfassung");
        this.withCommand("--max_connections=1000", "--shared_buffers=240MB");
        this.setWaitStrategy(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10)));
    }

    /**
     * Sets the spring datasource configuration properties.
     *
     * <p>Usage:</p>
     * <pre><code>
     * static final SingleTenantPostgreSQLContainer postgres = new SingleTenantPostgreSQLContainer();
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
        // privileged user from postgres to bypass row-level-security
        registry.add("spring.datasource.url", this::getJdbcUrl);
        registry.add("spring.datasource.username", this::getUsername);
        registry.add("spring.datasource.password", this::getPassword);
    }
}
