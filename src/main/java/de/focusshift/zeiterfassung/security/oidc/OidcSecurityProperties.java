package de.focusshift.zeiterfassung.security.oidc;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.security.oidc")
public class OidcSecurityProperties {

    /**
     * OIDC post logout redirect uri.
     * <p>
     * Redirects the user to the given url after logout.
     * Default is the base url of the request.
     */
    @NotEmpty
    private String postLogoutRedirectUri = "{baseUrl}";

    @NotEmpty
    private String loginFormUrl;

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public String getLoginFormUrl() {
        return loginFormUrl;
    }

    public void setLoginFormUrl(String loginFormUrl) {
        this.loginFormUrl = loginFormUrl;
    }
}
