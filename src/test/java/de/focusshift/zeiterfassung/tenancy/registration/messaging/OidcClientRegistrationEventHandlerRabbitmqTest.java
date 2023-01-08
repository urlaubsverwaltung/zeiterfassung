package de.focusshift.zeiterfassung.tenancy.registration.messaging;


import de.focusshift.zeiterfassung.tenancy.registration.web.TenantRegistration;
import de.focusshift.zeiterfassung.tenancy.registration.web.TenantRegistrationService;
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
