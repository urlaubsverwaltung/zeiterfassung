package de.focusshift.zeiterfassung.security.oidc.clientregistration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.MULTI;
import static java.lang.String.format;

@Component
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
class KeycloakUrlProvider {

    private final String keycloakServerUrl;

    KeycloakUrlProvider(OidcClientRegistrationConfigurationProperties tenantRegistrationConfigurationProperties) {
        this.keycloakServerUrl = tenantRegistrationConfigurationProperties.getServerUrl();
    }

    public String keycloakTenantRealmUrl(String tenantId) {
        return format("%s/realms/%s", keycloakServerUrl, tenantId);
    }

    public String jkwSetUri(String tenantId) {
        return format("%s/protocol/openid-connect/certs", keycloakTenantRealmUrl(tenantId));
    }

    public String userInfoUri(String tenantId) {
        return format("%s/protocol/openid-connect/userinfo", keycloakTenantRealmUrl(tenantId));
    }

    public String authorizationUri(String tenantId) {
        return format("%s/protocol/openid-connect/auth", keycloakTenantRealmUrl(tenantId));
    }

    public String tokenUri(String tenantId) {
        return format("%s/protocol/openid-connect/token", keycloakTenantRealmUrl(tenantId));
    }

    public String userRegistrationUri(String tenantId) {
        return format("%s/protocol/openid-connect/registrations", keycloakTenantRealmUrl(tenantId));
    }

    public String logoutUri(String tenantId) {
        return format("%s/protocol/openid-connect/logout", keycloakTenantRealmUrl(tenantId));
    }
}
