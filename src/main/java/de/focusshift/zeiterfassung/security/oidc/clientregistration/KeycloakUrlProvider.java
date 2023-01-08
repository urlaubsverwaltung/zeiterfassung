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

    String keycloakTenantRealmUrl(String tenantId) {
        return format("%s/realms/%s", keycloakServerUrl, tenantId);
    }

    String jkwSetUri(String tenantId) {
        return format("%s/protocol/openid-connect/certs", keycloakTenantRealmUrl(tenantId));
    }

    String userInfoUri(String tenantId) {
        return format("%s/protocol/openid-connect/userinfo", keycloakTenantRealmUrl(tenantId));
    }

    String authorizationUri(String tenantId) {
        return format("%s/protocol/openid-connect/auth", keycloakTenantRealmUrl(tenantId));
    }

    String tokenUri(String tenantId) {
        return format("%s/protocol/openid-connect/token", keycloakTenantRealmUrl(tenantId));
    }

    String userRegistrationUri(String tenantId) {
        return format("%s/protocol/openid-connect/registrations", keycloakTenantRealmUrl(tenantId));
    }

    String logoutUri(String tenantId) {
        return format("%s/protocol/openid-connect/logout", keycloakTenantRealmUrl(tenantId));
    }
}
