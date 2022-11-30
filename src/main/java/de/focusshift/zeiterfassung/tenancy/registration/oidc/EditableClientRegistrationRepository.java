package de.focusshift.zeiterfassung.tenancy.registration.oidc;

import de.focusshift.zeiterfassung.tenancy.registration.oidc.persistent.OidcClientEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.List;

public interface EditableClientRegistrationRepository extends ClientRegistrationRepository {

    ClientRegistration addNewClient(String tenantId, String clientSecret);

    boolean existsClient(String tenantId);

    void deleteExistingClient(String tenantId);

    List<OidcClientEntity> findAll();
}
