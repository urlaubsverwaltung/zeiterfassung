package de.focusshift.zeiterfassung.tenancy.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantContextRunnerTest {

    private TenantContextRunner sut;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        sut = new TenantContextRunner(tenantContextHolder, tenantService);
    }

    @Test
    void ensureRunsFunctionForEveryActiveTenant() {

        final Tenant tenantOne = tenant("one", TenantStatus.ACTIVE);
        final Tenant tenantTwo = tenant("two", TenantStatus.ACTIVE);
        final Tenant tenantThree = tenant("three", TenantStatus.DISABLED);

        when(tenantService.findAllTenants()).thenReturn(List.of(tenantOne, tenantTwo, tenantThree));

        final Runnable function = () -> {};
        sut.runForEachActiveTenant(function).run();

        verify(tenantContextHolder).runInTenantIdContext(eq(new TenantId("one")), eq(function));
        verify(tenantContextHolder).runInTenantIdContext(eq(new TenantId("two")), eq(function));
        verify(tenantContextHolder, times(0)).runInTenantIdContext(eq(new TenantId("three")), any(Runnable.class));
    }

    @Test
    void ensureExceptionForOneTenantDoesNotAbortRemainingTenants() {

        final Tenant tenantOne = tenant("one", TenantStatus.ACTIVE);
        final Tenant tenantTwo = tenant("two", TenantStatus.ACTIVE);

        when(tenantService.findAllTenants()).thenReturn(List.of(tenantOne, tenantTwo));

        final Runnable function = () -> {};

        doThrow(new IllegalStateException("boom"))
            .when(tenantContextHolder).runInTenantIdContext(eq(new TenantId("one")), eq(function));

        sut.runForEachActiveTenant(function).run();

        verify(tenantContextHolder).runInTenantIdContext(eq(new TenantId("one")), eq(function));
        verify(tenantContextHolder).runInTenantIdContext(eq(new TenantId("two")), eq(function));
    }

    private static Tenant tenant(String tenantId, TenantStatus status) {
        return new Tenant(tenantId, Instant.now(), Instant.now(), status);
    }
}
