package de.focusshift.zeiterfassung.tenancy.tenant;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;

@MappedSuperclass
@EntityListeners(TenantListener.class)
public abstract class AbstractTenantAwareEntity implements TenantAware, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 255)
    @Column(name = "tenant_id")
    private String tenantId;

    protected AbstractTenantAwareEntity(String tenantId) {
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
