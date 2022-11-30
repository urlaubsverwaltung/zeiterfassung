package de.focusshift.zeiterfassung.registration.oidc;

import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Validated
@ConfigurationProperties("zeiterfassung.tenant.registration")
public class TenantRegistrationConfigurationProperties {


    /**
     * to enable or disable the generation of tenants based on the oidc clients
     */
    private boolean enabled;

    @URL
    @NotEmpty
    private String serverUrl;

    @NotEmpty
    private String redirectUriTemplate;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getRedirectUriTemplate() {
        return redirectUriTemplate;
    }

    public void setRedirectUriTemplate(String redirectUriTemplate) {
        this.redirectUriTemplate = redirectUriTemplate;
    }

}
