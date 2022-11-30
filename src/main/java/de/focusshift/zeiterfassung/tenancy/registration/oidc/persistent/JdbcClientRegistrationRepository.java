package de.focusshift.zeiterfassung.tenancy.registration.oidc.persistent;

import de.focusshift.zeiterfassung.tenancy.registration.oidc.EditableClientRegistrationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Component;

import java.util.List;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.MULTI;

@Component
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
public class JdbcClientRegistrationRepository implements EditableClientRegistrationRepository {

    private final OidcClientEntityRepository oidcClientEntityRepository;
    private final OidcClientEntityToClientRegistrationConverter converter;

    public JdbcClientRegistrationRepository(OidcClientEntityRepository oidcClientEntityRepository,
                                            OidcClientEntityToClientRegistrationConverter oidcClientEntityToClientRegistrationConverter) {
        this.converter = oidcClientEntityToClientRegistrationConverter;
        this.oidcClientEntityRepository = oidcClientEntityRepository;
    }

    @Override
    public ClientRegistration findByRegistrationId(String tenantId) {
        final OidcClientEntity byTenantId = oidcClientEntityRepository.findByTenantId(tenantId);
        return converter.convert(byTenantId);
    }

    @Override
    public ClientRegistration addNewClient(String tenantId, String clientSecret) {
        if (!existsClient(tenantId)) {
            final OidcClientEntity oidcClientEntity = new OidcClientEntity();
            oidcClientEntity.setTenantId(tenantId);
            oidcClientEntity.setClientSecret(clientSecret);
            oidcClientEntityRepository.save(oidcClientEntity);
        }

        return findByRegistrationId(tenantId);
    }

    @Override
    public void deleteExistingClient(String tenantId) {
        if (!existsClient(tenantId)) {
            return;
        }
        oidcClientEntityRepository.delete(oidcClientEntityRepository.findByTenantId(tenantId));
    }

    @Override
    public List<OidcClientEntity> findAll() {
        return oidcClientEntityRepository.findAll();
    }

    @Override
    public boolean existsClient(String tenantId) {
        return tenantId != null && oidcClientEntityRepository.findByTenantId(tenantId) != null;
    }
}
