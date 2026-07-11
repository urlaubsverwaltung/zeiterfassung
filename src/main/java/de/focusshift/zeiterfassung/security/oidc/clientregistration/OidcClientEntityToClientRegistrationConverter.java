package de.focusshift.zeiterfassung.security.oidc.clientregistration;

import de.focusshift.zeiterfassung.tenancy.configuration.multi.ConditionalOnMultiTenantMode;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMultiTenantMode
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
