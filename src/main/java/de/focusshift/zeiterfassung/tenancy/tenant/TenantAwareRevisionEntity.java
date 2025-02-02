package de.focusshift.zeiterfassung.tenancy.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Size;
import org.hibernate.envers.DateTimeFormatter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * Envers Revision Entity extended with custom data like `updated_by` and `tenant_id`.
 *
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(TenantRevisionListener.class)
public class TenantAwareRevisionEntity implements Serializable, TenantAware {

    @Serial
    private static final long serialVersionUID = 6793182106918370242L;

    @Id
    @GeneratedValue
    @RevisionNumber
    private long id;

    @RevisionTimestamp
    private long timestamp;

    @Size(max = 255)
    @Column(name = "tenant_id")
    private String tenantId;

    /**
     * optional field referencing tenant_user who updated this revision.
     */
    @Column(name = "updated_by")
    private String updatedBy;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Transient
    public Date getRevisionDate() {
        return new Date( timestamp );
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

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
        if (o == null || getClass() != o.getClass()) return false;
        TenantAwareRevisionEntity entity = (TenantAwareRevisionEntity) o;
        return id == entity.id && timestamp == entity.timestamp && Objects.equals(tenantId, entity.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, tenantId);
    }

    @Override
    public String toString() {
        return "TenantAwareRevisionEntity{" +
            "id=" + id +
            ", revisionDate=" + DateTimeFormatter.INSTANCE.format(getRevisionDate() ) +
            ", tenantId='" + tenantId + '\'' +
            ", updatedBy='" + updatedBy + '\'' +
            '}';
    }
}
