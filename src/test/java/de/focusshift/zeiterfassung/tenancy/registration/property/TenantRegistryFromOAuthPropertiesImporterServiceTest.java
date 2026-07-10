package de.focusshift.zeiterfassung.tenancy.registration.property;

import de.focusshift.zeiterfassung.tenancy.registration.TenantRegistration;
import de.focusshift.zeiterfassung.tenancy.registration.TenantRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantRegistryFromOAuthPropertiesImporterServiceTest {

    private TenantRegistryFromOAuthPropertiesImporterService sut;

    @Mock
    private OAuth2ClientProperties oAuth2ClientProperties;

    @Mock
    private TenantRegistrationService tenantRegistrationService;

    @BeforeEach
    void setUp() {
        sut = new TenantRegistryFromOAuthPropertiesImporterService(oAuth2ClientProperties, tenantRegistrationService);
    }

    @Test
    void ensureRegistersEveryTenantFromProperties() {

        final Map<String, OAuth2ClientProperties.Registration> registrations = new LinkedHashMap<>();
        registrations.put("one", registration("one", "secret-one"));
        registrations.put("two", registration("two", "secret-two"));

        when(oAuth2ClientProperties.getRegistration()).thenReturn(registrations);

        sut.importOIDCClientsFromProperties();

        verify(tenantRegistrationService).registerNewTenant(new TenantRegistration("one", "secret-one"));
        verify(tenantRegistrationService).registerNewTenant(new TenantRegistration("two", "secret-two"));
    }

    @Test
    void ensureExceptionForOneRegistrationDoesNotAbortRemainingRegistrations() {

        final Map<String, OAuth2ClientProperties.Registration> registrations = new LinkedHashMap<>();
        registrations.put("one", registration("one", "secret-one"));
        registrations.put("two", registration("two", "secret-two"));

        when(oAuth2ClientProperties.getRegistration()).thenReturn(registrations);

        doThrow(new IllegalStateException("boom"))
            .when(tenantRegistrationService).registerNewTenant(new TenantRegistration("one", "secret-one"));

        sut.importOIDCClientsFromProperties();

        verify(tenantRegistrationService).registerNewTenant(new TenantRegistration("one", "secret-one"));
        verify(tenantRegistrationService).registerNewTenant(new TenantRegistration("two", "secret-two"));
    }

    private static OAuth2ClientProperties.Registration registration(String provider, String clientSecret) {
        final OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
        registration.setProvider(provider);
        registration.setClientSecret(clientSecret);
        return registration;
    }
}
