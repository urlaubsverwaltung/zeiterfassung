package de.focusshift.zeiterfassung.security.oidc.clientregistration;

import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Validated
@ConfigurationProperties("zeiterfassung.security.oidc.client.registration")
public class OidcClientRegistrationConfigurationProperties {

    @URL
    @NotEmpty
    private String serverUrl;

    @NotEmpty
    private String redirectUriTemplate;

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
