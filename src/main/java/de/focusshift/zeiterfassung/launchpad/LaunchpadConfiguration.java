package de.focusshift.zeiterfassung.launchpad;

import de.focus_shift.launchpad.tenancy.LaunchpadTenantConfiguration;
import de.focus_shift.launchpad.tenancy.TenantSupplier;
import de.focusshift.zeiterfassung.tenancy.configuration.multi.ConditionalOnMultiTenantMode;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnMultiTenantMode
@Import(LaunchpadTenantConfiguration.class)
class LaunchpadConfiguration {

    @Bean
    TenantSupplier tenantSupplier(TenantContextHolder tenantContextHolder) {
        return new ZeiterfassungTenantSupplier(tenantContextHolder);
    }
}
