package de.focusshift.zeiterfassung.tenancy.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import org.hibernate.envers.Audited;

import java.io.Serial;
import java.io.Serializable;

/**
 * Pendant to {@link AbstractTenantAwareEntity} but for entities using hibernate {@link Audited}.
 */
@Audited
@MappedSuperclass
@EntityListeners(TenantListener.class)
public abstract class AbstractTenantAwareAuditedEntity implements TenantAware, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 255)
    @Column(name = "tenant_id")
    private String tenantId;

    protected AbstractTenantAwareAuditedEntity(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
