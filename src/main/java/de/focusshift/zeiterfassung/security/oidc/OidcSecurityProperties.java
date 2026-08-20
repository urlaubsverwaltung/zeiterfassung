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

    /**
     * Whether to force retrieve user info from the OIDC provider.
     */
    private boolean retrieveUserInfo;

    /**
     * OIDC end_session_endpoint used for RP-initiated logout.
     * <p>
     * This is only needed when the provider's {@code end_session_endpoint} cannot be discovered
     * automatically, e.g. when the OAuth2 client is configured with explicit
     * {@code authorization-uri}/{@code token-uri}/{@code jwk-set-uri}/{@code user-info-uri} instead
     * of {@code issuer-uri} (no OpenID Connect discovery happens in that case).
     */
    private String endSessionEndpoint;

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

    public void setRetrieveUserInfo(boolean retrieveUserInfo) {
        this.retrieveUserInfo = retrieveUserInfo;
    }

    public boolean retrieveUserInfo() {
        return retrieveUserInfo;
    }

    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    public void setEndSessionEndpoint(String endSessionEndpoint) {
        this.endSessionEndpoint = endSessionEndpoint;
    }
}
