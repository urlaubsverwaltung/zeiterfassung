package de.focusshift.zeiterfassung.tenancy.registration.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.tenant.registration.property.oauth")
class TenantRegistryFromOAuthConfigurationProperties {

    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
