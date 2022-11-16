package de.focusshift.zeiterfassung.integration.oidc.clientregistration.messaging;


import de.focusshift.zeiterfassung.integration.oidc.clientregistration.OidcClientCreatedEventDTO;
import de.focusshift.zeiterfassung.integration.oidc.clientregistration.OidcClientDeletedEventDTO;
import de.focusshift.zeiterfassung.registration.tenant.TenantRegistration;
import de.focusshift.zeiterfassung.registration.tenant.TenantRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OidcClientRegistrationEventHandlerRabbitmqTest {

    private OidcClientRegistrationEventHandlerRabbitmq sut;

    @Mock
    private TenantRegistrationService tenantRegistrationService;

    @BeforeEach
    void setUp() {
        sut = new OidcClientRegistrationEventHandlerRabbitmq(tenantRegistrationService);
    }

    @Test
    void ensureHandleCreatedEventCallsRegisterNewTenant() {
        sut.handleEvent(new OidcClientCreatedEventDTO("tenantId", "clientId", "clientSecret"));
        verify(tenantRegistrationService).registerNewTenant(new TenantRegistration("tenantId", "clientSecret"));
    }

    @Test
    void ensureHandleDeletedEventCallsRegisterNewTenant() {
        sut.handleEvent(new OidcClientDeletedEventDTO("tenantId", "clientId"));
        verify(tenantRegistrationService).disableTenant("tenantId");
    }
}
