package de.focusshift.zeiterfassung.tenancy.tenant;

import org.springframework.stereotype.Component;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

@Component
class TenantListener {

    private final TenantContextHolder tenantContextHolder;

    public TenantListener(TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
    }

    @PreUpdate
    @PreRemove
    @PrePersist
    public void setTenant(Object entity) {
        if (entity instanceof TenantAware tenantAware) {
            final String tenantId = tenantContextHolder.getCurrentTenantId()
                .map(TenantId::tenantId)
                .orElseThrow(() -> new MissingTenantException("No tenant found in security context"));

            tenantAware.setTenantId(tenantId);
        }
    }
}
