package de.focusshift.zeiterfassung.tenancy.tenant;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class TenantContextRunner {

    private static final Logger LOG = getLogger(TenantContextRunner.class);

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
        return () -> getAllActiveTenants().forEach(tenant -> {
            try {
                tenantContextHolder.runInTenantIdContext(new TenantId(tenant.tenantId()), function);
            } catch (Exception exception) {
                LOG.error("Unexpected error while running function for tenant={}. Continuing with remaining tenants.", tenant.tenantId(), exception);
            }
        });
    }

    private Stream<Tenant> getAllActiveTenants() {
        return tenantService.findAllTenants()
            .stream()
            .filter(tenant -> tenant.status().equals(TenantStatus.ACTIVE));
    }
}
