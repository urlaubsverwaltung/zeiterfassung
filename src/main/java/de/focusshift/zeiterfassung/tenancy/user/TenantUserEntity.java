package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.Objects;

import static javax.persistence.GenerationType.SEQUENCE;

@Entity
@Table(name = "tenant_user")
public class TenantUserEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "tenant_user_seq", sequenceName = "tenant_user_seq")
    @GeneratedValue(strategy = SEQUENCE, generator = "tenant_user_seq")
    protected Long id;

    @Column(name = "uuid", length = 36, nullable = false)
    @NotNull
    @Size(max = 36)
    private String uuid;

    @Column(name = "first_login_at", nullable = false)
    @NotNull
    private Instant firstLoginAt;

    @Column(name = "last_login_at", nullable = false)
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

    protected TenantUserEntity() {
        this(null, null, null, null, null, null, null, null);
    }

    protected TenantUserEntity(Long id, String uuid, Instant firstLoginAt, Instant lastLoginAt, String givenName, String familyName, String email) {
        this(id, null, uuid, firstLoginAt, lastLoginAt, givenName, familyName, email);
    }

    protected TenantUserEntity(Long id, String tenantId, String uuid, Instant firstLoginAt, Instant lastLoginAt, String givenName, String familyName, String email) {
        super(tenantId);
        this.id = id;
        this.uuid = uuid;
        this.firstLoginAt = firstLoginAt;
        this.lastLoginAt = lastLoginAt;
        this.givenName = givenName;
        this.familyName = familyName;
        this.email = email;
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
