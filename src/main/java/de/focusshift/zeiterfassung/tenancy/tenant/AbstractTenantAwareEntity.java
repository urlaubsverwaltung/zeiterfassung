package de.focusshift.zeiterfassung.tenancy.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.envers.NotAudited;

import java.io.Serial;
import java.io.Serializable;

@MappedSuperclass
@EntityListeners(TenantListener.class)
public abstract class AbstractTenantAwareEntity implements TenantAware, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotAudited
    @NotNull
    @Size(max = 255)
    @Column(name = "tenant_id", nullable = false)
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
