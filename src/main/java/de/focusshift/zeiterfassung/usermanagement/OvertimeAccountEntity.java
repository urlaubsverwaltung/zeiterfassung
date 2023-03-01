package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.util.Objects;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "overtime_account")
public class OvertimeAccountEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = LAZY)
    @PrimaryKeyJoinColumn(name = "user_id", referencedColumnName = "user_id")
    private TenantUserEntity user;

    private boolean allowed;

    private String maxAllowedOvertime;

    protected OvertimeAccountEntity() {
        super(null);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public TenantUserEntity getUser() {
        return user;
    }

    public void setUser(TenantUserEntity user) {
        this.user = user;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public String getMaxAllowedOvertime() {
        return maxAllowedOvertime;
    }

    public void setMaxAllowedOvertime(String maxAllowedOvertime) {
        this.maxAllowedOvertime = maxAllowedOvertime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OvertimeAccountEntity that = (OvertimeAccountEntity) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
