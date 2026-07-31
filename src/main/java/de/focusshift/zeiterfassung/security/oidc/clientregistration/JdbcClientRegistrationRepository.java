package de.focusshift.zeiterfassung.security.oidc.clientregistration;

import de.focusshift.zeiterfassung.tenancy.configuration.multi.ConditionalOnMultiTenantMode;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMultiTenantMode
class JdbcClientRegistrationRepository implements ClientRegistrationRepository {

    private final OidcClientEntityRepository oidcClientEntityRepository;
    private final OidcClientEntityToClientRegistrationConverter converter;

    JdbcClientRegistrationRepository(OidcClientEntityRepository oidcClientEntityRepository,
                                     OidcClientEntityToClientRegistrationConverter oidcClientEntityToClientRegistrationConverter) {
        this.converter = oidcClientEntityToClientRegistrationConverter;
        this.oidcClientEntityRepository = oidcClientEntityRepository;
    }

    @Override
    public ClientRegistration findByRegistrationId(String tenantId) {
        final OidcClientEntity byTenantId = oidcClientEntityRepository.findByTenantId(tenantId);
        return converter.convert(byTenantId);
    }

    ClientRegistration addNewClient(String tenantId, String clientSecret) {
        if (!existsClient(tenantId)) {
            final OidcClientEntity oidcClientEntity = new OidcClientEntity();
            oidcClientEntity.setTenantId(tenantId);
            oidcClientEntity.setClientSecret(clientSecret);
            oidcClientEntityRepository.save(oidcClientEntity);
        }

        return findByRegistrationId(tenantId);
    }

    void deleteExistingClient(String tenantId) {
        if (!existsClient(tenantId)) {
            return;
        }
        oidcClientEntityRepository.delete(oidcClientEntityRepository.findByTenantId(tenantId));
    }

    boolean existsClient(String tenantId) {
        return tenantId != null && oidcClientEntityRepository.findByTenantId(tenantId) != null;
    }
}
