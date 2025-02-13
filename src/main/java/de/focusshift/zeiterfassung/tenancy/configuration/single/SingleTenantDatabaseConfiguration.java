package de.focusshift.zeiterfassung.tenancy.configuration.single;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.SINGLE;

@Configuration
@EnableEnversRepositories(basePackages="de.focusshift.zeiterfassung")
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = SINGLE, matchIfMissing = true)
class SingleTenantDatabaseConfiguration {

}
