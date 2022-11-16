package de.focusshift.zeiterfassung.multitenant;

import de.focusshift.zeiterfassung.tenant.SingleTenantConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static de.focusshift.zeiterfassung.tenant.TenantConfigurationProperties.SINGLE;

@Component
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = SINGLE, matchIfMissing = true)
public class TenantContextHolderSingleTenant implements TenantContextHolder {

    private final String defaultTenantId;

    public TenantContextHolderSingleTenant(SingleTenantConfigurationProperties singleTenantConfigurationProperties) {
        this.defaultTenantId = singleTenantConfigurationProperties.getDefaultTenantId();
    }

    public Optional<TenantId> getCurrentTenantId() {
        return Optional.of(new TenantId(defaultTenantId));
    }
}
