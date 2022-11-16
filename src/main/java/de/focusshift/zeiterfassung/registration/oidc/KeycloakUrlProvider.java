package de.focusshift.zeiterfassung.registration.oidc;

import org.springframework.stereotype.Component;

@Component
public class KeycloakUrlProvider {

    private final String keycloakServerUrl;

    public KeycloakUrlProvider(SecurityConfigurationProperties securityConfigurationProperties) {
        this.keycloakServerUrl = securityConfigurationProperties.getServerUrl();
    }

    public String keycloakTenantRealmUrl(String tenantId) {
        return String.format("%s/realms/%s", keycloakServerUrl, tenantId);
    }

    public String jkwSetUri(String tenantId) {
        return String.format("%s/protocol/openid-connect/certs", keycloakTenantRealmUrl(tenantId));
    }

    public String userInfoUri(String tenantId) {
        return String.format("%s/protocol/openid-connect/userinfo", keycloakTenantRealmUrl(tenantId));
    }

    public String authorizationUri(String tenantId) {
        return String.format("%s/protocol/openid-connect/auth", keycloakTenantRealmUrl(tenantId));
    }

    public String tokenUri(String tenantId) {
        return String.format("%s/protocol/openid-connect/token", keycloakTenantRealmUrl(tenantId));
    }

    public String userRegistrationUri(String tenantId) {
        return String.format("%s/protocol/openid-connect/registrations", keycloakTenantRealmUrl(tenantId));
    }

    public String logoutUri(String tenantId) {
        return String.format("%s/protocol/openid-connect/logout", keycloakTenantRealmUrl(tenantId));
    }
}
