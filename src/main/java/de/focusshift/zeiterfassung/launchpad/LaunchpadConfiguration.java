package de.focusshift.zeiterfassung.launchpad;

import de.focus_shift.launchpad.tenancy.LaunchpadTenantConfiguration;
import de.focus_shift.launchpad.tenancy.TenantSupplier;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.MULTI;

@Configuration
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
@Import(LaunchpadTenantConfiguration.class)
class LaunchpadConfiguration {

    @Bean
    TenantSupplier tenantSupplier(TenantContextHolder tenantContextHolder) {
        return new ZeiterfassungTenantSupplier(tenantContextHolder);
    }
}
