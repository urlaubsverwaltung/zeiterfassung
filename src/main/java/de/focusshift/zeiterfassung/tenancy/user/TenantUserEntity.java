package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.GenerationType.SEQUENCE;

@Entity
@Table(name = "tenant_user")
public class TenantUserEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "tenant_user_seq", sequenceName = "tenant_user_seq")
    @GeneratedValue(strategy = SEQUENCE, generator = "tenant_user_seq")
    protected Long id;

    @Column(name = "uuid", nullable = false)
    @NotNull
    private String uuid;

    @Column(name = "first_login_at")
    @NotNull
    private Instant firstLoginAt;

    @Column(name = "last_login_at")
    @NotNull
    private Instant lastLoginAt;

    @Column(name = "given_name", nullable = false)
    @NotNull
    @Size(max = 255)
    private String givenName;

    @Column(name = "family_name", nullable = false)
    @NotNull
    @Size(max = 255)
    private String familyName;

    @Column(name = "email", nullable = false)
    @NotNull
    @Size(max = 255)
    private String email;

    @CollectionTable(name = "tenant_user_authorities", joinColumns = @JoinColumn(name = "tenant_user_id"))
    @ElementCollection(fetch = EAGER)
    @Enumerated(STRING)
    private Set<SecurityRole> authorities;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "status", nullable = false)
    @Enumerated(STRING)
    private UserStatus status;

    protected TenantUserEntity() {
        this(null, null, null, null, null, null, null, Set.of(), null, null, null, null, UserStatus.UNKNOWN);
    }

    public TenantUserEntity(Long id, String uuid, Instant firstLoginAt, Instant lastLoginAt, String givenName, String familyName, String email, Set<SecurityRole> authorities, Instant createdAt, Instant updatedAt, Instant deactivatedAt, Instant deletedAt, UserStatus status) {
        this(id, null, uuid, firstLoginAt, lastLoginAt, givenName, familyName, email, authorities, createdAt, updatedAt, deactivatedAt, deletedAt, status);
    }

    protected TenantUserEntity(Long id, String tenantId, String uuid, Instant firstLoginAt, Instant lastLoginAt, String givenName, String familyName, String email, Set<SecurityRole> authorities, Instant createdAt, Instant updatedAt, Instant deactivatedAt, Instant deletedAt, UserStatus status) {
        super(tenantId);
        this.id = id;
        this.uuid = uuid;
        this.firstLoginAt = firstLoginAt;
        this.lastLoginAt = lastLoginAt;
        this.givenName = givenName;
        this.familyName = familyName;
        this.email = email;
        this.authorities = authorities;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deactivatedAt = deactivatedAt;
        this.deletedAt = deletedAt;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public Instant getFirstLoginAt() {
        return firstLoginAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getEmail() {
        return email;
    }

    public Set<SecurityRole> getAuthorities() {
        return authorities;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public UserStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantUserEntity that = (TenantUserEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
