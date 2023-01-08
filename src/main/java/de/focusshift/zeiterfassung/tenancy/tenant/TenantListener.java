package de.focusshift.zeiterfassung.tenancy.tenant;

import org.springframework.stereotype.Component;

import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

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
