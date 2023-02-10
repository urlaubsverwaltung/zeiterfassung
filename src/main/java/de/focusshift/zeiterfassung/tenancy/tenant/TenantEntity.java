package de.focusshift.zeiterfassung.tenancy.tenant;

import de.focusshift.zeiterfassung.tenancy.configuration.multi.AdminAware;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.SEQUENCE;

@Entity
@Table(name = "tenant")
public class TenantEntity implements AdminAware<Long> {

    enum TenantStatusEntity {
        ACTIVE, DISABLED, ARCHIVED, DELETED;
    }

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "tenant_seq", sequenceName = "tenant_seq")
    @GeneratedValue(strategy = SEQUENCE, generator = "tenant_seq")
    protected Long id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    @NotNull
    @Size(max = 36)
    private String tenantId;

    @Column(name = "created_at", nullable = false)
    @NotNull
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @NotNull
    private Instant updatedAt;

    @Enumerated(STRING)
    @Column(nullable = false)
    private TenantStatusEntity status;

    protected TenantEntity() {
        this(null, null, null, null, null);
    }

    public TenantEntity(Long id, String tenantId, Instant createdAt, Instant updatedAt, TenantStatusEntity status) {
        this.id = id;
        this.tenantId = tenantId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.status = status;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public TenantStatusEntity getStatus() {
        return status;
    }

    public void setStatus(TenantStatusEntity status) {
        this.status = status;
    }
}
