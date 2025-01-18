package de.focusshift.zeiterfassung.tenancy.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

@Entity
@Table(name = "revinfo")
@RevisionEntity(TenantRevisionListener.class)
public class TenantAwareRevisionEntity extends DefaultRevisionEntity implements TenantAware {

    @Size(max = 255)
    @Column(name = "tenant_id")
    private String tenantId;

    /**
     * optional field referencing tenant_user who updated this revision.
     */
    @Column(name = "updated_by")
    private String updatedBy;

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedByTenantUser) {
        this.updatedBy = updatedByTenantUser;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
