package de.focusshift.zeiterfassung.tenancy.tenant;

import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
public class TenantContextRunner {

    private final TenantContextHolder tenantContextHolder;
    private final TenantService tenantService;

    TenantContextRunner(
        TenantContextHolder tenantContextHolder,
        TenantService tenantService
    ) {
        this.tenantContextHolder = tenantContextHolder;
        this.tenantService = tenantService;
    }

    /**
     * Pass a runnable function that will be executed for every active tenant
     * and preconfigured tenant context
     *
     * @param function to run for each tenant
     * @return a runnable
     */
    public Runnable runForEachActiveTenant(Runnable function) {
        return () -> getAllActiveTenants().forEach(tenant -> tenantContextHolder.runInTenantIdContext(new TenantId(tenant.tenantId()), function));
    }

    private Stream<Tenant> getAllActiveTenants() {
        return tenantService.findAllTenants()
            .stream()
            .filter(tenant -> tenant.status().equals(TenantStatus.ACTIVE));
    }
}
