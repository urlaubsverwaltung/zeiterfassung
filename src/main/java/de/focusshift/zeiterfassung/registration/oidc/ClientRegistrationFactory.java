package de.focusshift.zeiterfassung.registration.oidc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;

import java.util.Map;

import static de.focusshift.zeiterfassung.tenant.TenantConfigurationProperties.MULTI;

@Component
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
@EnableConfigurationProperties(TenantRegistrationConfigurationProperties.class)
public class ClientRegistrationFactory {

    private final KeycloakUrlProvider keycloakUrlProvider;
    private final String redirectUriTemplate;

    public ClientRegistrationFactory(KeycloakUrlProvider keycloakUrlProvider, TenantRegistrationConfigurationProperties tenantRegistrationConfigurationProperties) {
        this.keycloakUrlProvider = keycloakUrlProvider;
        this.redirectUriTemplate = tenantRegistrationConfigurationProperties.getRedirectUriTemplate();
    }

    public ClientRegistration createClientRegistration(String tenantId, String clientSecret) {

        final String realmUrl = keycloakUrlProvider.keycloakTenantRealmUrl(tenantId);
        final String endsessionEndpoint = keycloakUrlProvider.logoutUri(tenantId);

        final Map<String, Object> configurationMetadata = Map.of("end_session_endpoint", endsessionEndpoint);

        return ClientRegistration.withRegistrationId(tenantId)
            .providerConfigurationMetadata(configurationMetadata)
            .tokenUri(keycloakUrlProvider.tokenUri(tenantId))
            .authorizationUri(keycloakUrlProvider.authorizationUri(tenantId))
            .userInfoUri(keycloakUrlProvider.userInfoUri(tenantId))
            .jwkSetUri(keycloakUrlProvider.jkwSetUri(tenantId))
            .registrationId(tenantId)
            .clientId("zeiterfassung")
            .clientName("zeiterfassung")
            .clientSecret(clientSecret)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .issuerUri(realmUrl)
            .redirectUri(redirectUriTemplate)
            .scope("openid", "profile", "email", "roles")
            .userNameAttributeName("preferred_username")
            .build();
    }
}
