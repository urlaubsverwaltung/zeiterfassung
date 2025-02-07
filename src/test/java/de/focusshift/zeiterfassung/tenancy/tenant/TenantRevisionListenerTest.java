package de.focusshift.zeiterfassung.tenancy.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantRevisionListenerTest {

    private TenantRevisionListener sut;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @BeforeEach
    void setUp() {
        sut = new TenantRevisionListener(tenantContextHolder);
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

        final Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("name");

        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> securityContextMock = mockStatic(SecurityContextHolder.class)) {
            securityContextMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            final TenantAwareRevisionEntity entity = mock(TenantAwareRevisionEntity.class);
            sut.newRevision(entity);

            verify(entity).setUpdatedBy("name");
        }
    }

    @Test
    void ensureNewRevisionDoesNotSetUpdatedBy() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant")));


        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);

        try (MockedStatic<SecurityContextHolder> securityContextMock = mockStatic(SecurityContextHolder.class)) {
            securityContextMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            final TenantAwareRevisionEntity entity = mock(TenantAwareRevisionEntity.class);
            sut.newRevision(entity);

            verify(entity, times(0)).setUpdatedBy(anyString());
        }
    }
}
