package de.focusshift.zeiterfassung.launchpad;

import de.focusshift.launchpad.api.LaunchpadAppUrlCustomizer;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.MULTI;

@Configuration
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
class LaunchpadConfiguration {

    @Bean
    LaunchpadAppUrlCustomizer launchpadAppUrlCustomizer(TenantContextHolder tenantContextHolder) {
        return url -> new URL(url.replace("{tenantId}", tenantContextHolder.getCurrentTenantId().map(TenantId::tenantId).orElseThrow()));
    }
}
