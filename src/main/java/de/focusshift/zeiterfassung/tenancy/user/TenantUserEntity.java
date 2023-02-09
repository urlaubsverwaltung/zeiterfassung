package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.security.SecurityRoles;
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
import org.hibernate.annotations.LazyCollection;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.SEQUENCE;
import static org.hibernate.annotations.LazyCollectionOption.FALSE;

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

    @CollectionTable(name = "tenant_user_authorities", joinColumns = @JoinColumn(name = "tenant_user_id"))
    @ElementCollection
    @LazyCollection(FALSE)
    @Enumerated(STRING)
    private Set<SecurityRoles> authorities;

    protected TenantUserEntity() {
        this(null, null, null, null, null, null, null, null, Set.of());
    }

    protected TenantUserEntity(Long id, String uuid, Instant firstLoginAt, Instant lastLoginAt, String givenName, String familyName, String email, Set<SecurityRoles> authorities) {
        this(id, null, uuid, firstLoginAt, lastLoginAt, givenName, familyName, email, authorities);
    }

    protected TenantUserEntity(Long id, String tenantId, String uuid, Instant firstLoginAt, Instant lastLoginAt, String givenName, String familyName, String email, Set<SecurityRoles> authorities) {
        super(tenantId);
        this.id = id;
        this.uuid = uuid;
        this.firstLoginAt = firstLoginAt;
        this.lastLoginAt = lastLoginAt;
        this.givenName = givenName;
        this.familyName = familyName;
        this.email = email;
        this.authorities = authorities;
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

    public Set<SecurityRoles> getAuthorities() {
        return authorities;
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
