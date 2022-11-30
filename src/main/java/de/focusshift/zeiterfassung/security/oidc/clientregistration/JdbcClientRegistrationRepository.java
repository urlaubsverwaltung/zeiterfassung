package de.focusshift.zeiterfassung.security.oidc.clientregistration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.MULTI;

@Component
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
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
