package de.focusshift.zeiterfassung.tenancy.configuration.single;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnSingleTenantMode
class TenantContextHolderSingleTenant implements TenantContextHolder {

    private final String defaultTenantId;

    TenantContextHolderSingleTenant(SingleTenantConfigurationProperties singleTenantConfigurationProperties) {
        this.defaultTenantId = singleTenantConfigurationProperties.getDefaultTenantId();
    }

    @Override
    public Optional<TenantId> getCurrentTenantId() {
        return Optional.of(new TenantId(defaultTenantId));
    }

}
