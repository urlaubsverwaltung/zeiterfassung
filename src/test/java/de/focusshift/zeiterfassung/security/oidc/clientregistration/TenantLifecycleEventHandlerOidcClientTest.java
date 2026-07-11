package de.focusshift.zeiterfassung.security.oidc.clientregistration;

import de.focusshift.zeiterfassung.tenancy.registration.TenantRegisteredEvent;
import de.focusshift.zeiterfassung.tenancy.tenant.Tenant;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantDisabledEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantLifecycleEventHandlerOidcClientTest {

    @InjectMocks
    private TenantLifecycleEventHandlerOidcClient sut;

    @Mock
    private JdbcClientRegistrationRepository jdbcClientRegistrationRepository;

    // --- handleTenantRegisteredEvent ---

    @Test
    void ensureRegistersNewClientWhenSecretProvidedAndClientDoesNotExist() {

        when(jdbcClientRegistrationRepository.existsClient("tenant-id")).thenReturn(false);

        sut.handleTenantRegisteredEvent(new TenantRegisteredEvent(tenant("tenant-id"), "super-secret"));

        verify(jdbcClientRegistrationRepository).addNewClient("tenant-id", "super-secret");
    }

    @Test
    void ensureSkipsRegistrationWhenClientAlreadyExists() {

        when(jdbcClientRegistrationRepository.existsClient("tenant-id")).thenReturn(true);

        sut.handleTenantRegisteredEvent(new TenantRegisteredEvent(tenant("tenant-id"), "super-secret"));

        verify(jdbcClientRegistrationRepository, never()).addNewClient(anyString(), anyString());
    }

    @Test
    void ensureSkipsRegistrationWhenNoClientSecretProvided() {

        sut.handleTenantRegisteredEvent(new TenantRegisteredEvent(tenant("tenant-id"), null));

        verifyNoInteractions(jdbcClientRegistrationRepository);
    }

    @Test
    void ensureSkipsRegistrationWhenClientSecretIsEmpty() {

        sut.handleTenantRegisteredEvent(new TenantRegisteredEvent(tenant("tenant-id"), ""));

        verifyNoInteractions(jdbcClientRegistrationRepository);
    }

    // --- handleTenantDisabledEvent ---

    @Test
    void ensureDeletesClientWhenClientExists() {

        when(jdbcClientRegistrationRepository.existsClient("tenant-id")).thenReturn(true);

        sut.handleTenantDisabledEvent(new TenantDisabledEvent(tenant("tenant-id")));

        verify(jdbcClientRegistrationRepository).deleteExistingClient("tenant-id");
    }

    @Test
    void ensureSkipsDeletionWhenClientDoesNotExist() {

        when(jdbcClientRegistrationRepository.existsClient("tenant-id")).thenReturn(false);

        sut.handleTenantDisabledEvent(new TenantDisabledEvent(tenant("tenant-id")));

        verify(jdbcClientRegistrationRepository, never()).deleteExistingClient(anyString());
    }

    private static Tenant tenant(String tenantId) {
        return new Tenant(tenantId, null, null, null);
    }
}
