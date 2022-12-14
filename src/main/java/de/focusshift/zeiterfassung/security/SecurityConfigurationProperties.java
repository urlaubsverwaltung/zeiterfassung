package de.focusshift.zeiterfassung.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Validated
@ConfigurationProperties("zeiterfassung.security.oidc")
public class SecurityConfigurationProperties {

    public static final String GROUPS = "groups";
    public static final String KEYCLOAK = "keycloak";

    public enum ClaimMappers {
        GROUPS,
        KEYCLOAK
    }

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

    @NotNull
    private ClaimMappers claimMapper;

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

    public ClaimMappers getClaimMapper() {
        return claimMapper;
    }

    public void setClaimMapper(ClaimMappers claimMapper) {
        this.claimMapper = claimMapper;
    }
}
