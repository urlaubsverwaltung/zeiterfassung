package de.focusshift.zeiterfassung;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class SingleTenantTestContainersBase {

    static final SingleTenantPostgreSQLContainer postgres = new SingleTenantPostgreSQLContainer();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        postgres.start();
        postgres.configureSpringDataSource(registry);
    }
}
