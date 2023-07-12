package de.focusshift.zeiterfassung.tenancy.configuration.single;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.SINGLE;

@Component
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = SINGLE, matchIfMissing = true)
class TenantContextHolderSingleTenant implements TenantContextHolder {

    private final String defaultTenantId;

    TenantContextHolderSingleTenant(SingleTenantConfigurationProperties singleTenantConfigurationProperties) {
        this.defaultTenantId = singleTenantConfigurationProperties.getDefaultTenantId();
    }

    @Override
    public Optional<TenantId> getCurrentTenantId() {
        return Optional.of(new TenantId(defaultTenantId));
    }

    @Override
    public void setTenantId(TenantId tenantId) {
        // do nothing
    }

    @Override
    public void clear() {
        // do nothing
    }
}
