package de.focusshift.zeiterfassung.tenancy.tenant;

import de.focusshift.zeiterfassung.security.AuthenticationFacade;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantRevisionListenerTest {

    private TenantRevisionListener sut;

    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private AuthenticationFacade authenticationFacade;

    @BeforeEach
    void setUp() {
        sut = new TenantRevisionListener(tenantContextHolder, authenticationFacade);
    }

    @Test
    void ensureNewRevisionDoesNotHandleOtherInstances() {

        final Object object = mock(Object.class);
        sut.newRevision(object);

        verifyNoInteractions(object);
        verifyNoInteractions(tenantContextHolder);
    }

    @Test
    void ensureNewRevisionThrowsWhenThereIsNoTenant() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.empty());

        final TenantAwareRevisionEntity entity = mock(TenantAwareRevisionEntity.class);

        assertThatThrownBy(() -> sut.newRevision(entity))
            .isInstanceOf(MissingTenantException.class)
            .hasMessage("No tenant found in security context");
    }

    @Test
    void ensureNewRevisionSetsTenantId() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant")));

        final TenantAwareRevisionEntity entity = mock(TenantAwareRevisionEntity.class);
        sut.newRevision(entity);

        verify(entity).setTenantId("tenant");
    }

    @Test
    void ensureNewRevisionSetsUpdatedBy() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant")));
        when(authenticationFacade.getCurrentUserIdComposite()).thenReturn(new UserIdComposite(new UserId("subject"), new UserLocalId(1L)));

        final TenantAwareRevisionEntity entity = mock(TenantAwareRevisionEntity.class);
        sut.newRevision(entity);

        verify(entity).setUpdatedBy("subject");
    }

    @Test
    void ensureNewRevisionDoesNotSetUpdatedBy() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant")));
        when(authenticationFacade.getCurrentUserIdComposite()).thenThrow(new IllegalStateException(""));

        final TenantAwareRevisionEntity entity = mock(TenantAwareRevisionEntity.class);
        sut.newRevision(entity);

        verify(entity, times(0)).setUpdatedBy(anyString());
    }
}
