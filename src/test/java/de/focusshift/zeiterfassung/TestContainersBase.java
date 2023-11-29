package de.focusshift.zeiterfassung;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.testcontainers.containers.PostgreSQLContainer.IMAGE;

public abstract class TestContainersBase {

    static final TestPostgreSQLContainer postgre = new TestPostgreSQLContainer();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        postgre.start();
        postgre.configureSpringDataSource(registry);
    }
}
