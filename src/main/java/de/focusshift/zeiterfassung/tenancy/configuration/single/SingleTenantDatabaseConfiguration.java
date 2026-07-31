package de.focusshift.zeiterfassung.tenancy.configuration.single;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;

@Configuration
@EnableEnversRepositories(basePackages="de.focusshift.zeiterfassung")
@ConditionalOnSingleTenantMode
class SingleTenantDatabaseConfiguration {

}
