package de.focusshift.zeiterfassung.security.oidc.clientregistration.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.security.oidc.client.registration.property")
class OidcClientRegistrationPropertyConfigurationProperties {

    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
