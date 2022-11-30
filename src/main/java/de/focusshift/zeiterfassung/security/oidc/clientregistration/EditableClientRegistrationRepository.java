package de.focusshift.zeiterfassung.security.oidc.clientregistration;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.List;

public interface EditableClientRegistrationRepository extends ClientRegistrationRepository {

    ClientRegistration addNewClient(String tenantId, String clientSecret);

    boolean existsClient(String tenantId);

    void deleteExistingClient(String tenantId);

    List<OidcClientEntity> findAll();
}
