package de.focusshift.zeiterfassung.tenancy.registration.oidc.persistent;

import de.focusshift.zeiterfassung.tenancy.registration.oidc.ClientRegistrationFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Component;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.MULTI;

@Component
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
public class OidcClientEntityToClientRegistrationConverter implements Converter<OidcClientEntity, ClientRegistration> {

    private final ClientRegistrationFactory clientRegistrationFactory;

    public OidcClientEntityToClientRegistrationConverter(ClientRegistrationFactory clientRegistrationFactory) {
        this.clientRegistrationFactory = clientRegistrationFactory;
    }

    @Override
    public ClientRegistration convert(OidcClientEntity oidcClientEntity) {
        return clientRegistrationFactory.createClientRegistration(oidcClientEntity.getTenantId(), oidcClientEntity.getClientSecret());
    }
}
