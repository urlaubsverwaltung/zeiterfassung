package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;

@Component
@ConditionalOnMultiTenantMode
class TenantContextHolderMultiTenant implements TenantContextHolder {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private static final InheritableThreadLocal<TenantId> currentTenantId = new InheritableThreadLocal<>();

    @Override
    public Optional<TenantId> getCurrentTenantId() {
        return Optional.ofNullable(currentTenantId.get());
    }

    @Override
    public void setTenantId(TenantId tenantId) {
        if (!tenantId.valid()) {
            throw new IllegalArgumentException("Invalid tenantId passed!");
        }

        LOG.debug("Setting tenantId to {}", tenantId);
        MDC.put("tenantId", tenantId.tenantId());
        currentTenantId.set(tenantId);
    }

    @Override
    public void clear() {
        LOG.debug("Clearing tenantId");
        MDC.remove("tenantId");
        currentTenantId.remove();
    }
}
