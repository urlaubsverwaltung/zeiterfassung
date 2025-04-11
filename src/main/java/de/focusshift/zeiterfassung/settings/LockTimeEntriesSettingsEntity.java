package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import java.util.Objects;

import static jakarta.persistence.EnumType.STRING;

@Entity(name = "settings_locking_time_entries")
public class LockTimeEntriesSettingsEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "settings_locking_time_entries_seq", sequenceName = "settings_locking_time_entries_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "settings_locking_time_entries_seq")
    protected Long id;

    @Column(name = "lock_time_entries_days_in_past")
    private int lockTimeEntriesDaysInPast;

    @Column(name = "locking_is_active")
    private boolean lockingIsActive;

    protected LockTimeEntriesSettingsEntity() {
        super(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getLockTimeEntriesDaysInPast() {
        return lockTimeEntriesDaysInPast;
    }

    public void setLockTimeEntriesDaysInPast(int lockTimeEntriesDaysInPast) {
        this.lockTimeEntriesDaysInPast = lockTimeEntriesDaysInPast;
    }

    public boolean isLockingIsActive() {
        return lockingIsActive;
    }

    public void setLockingIsActive(boolean lockingIsActive) {
        this.lockingIsActive = lockingIsActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LockTimeEntriesSettingsEntity that = (LockTimeEntriesSettingsEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LockTimeEntriesSettingsEntity{" +
            "id=" + id +
            ", lockTimeEntriesDaysInPast=" + lockTimeEntriesDaysInPast +
            ", lockingIsActive=" + lockingIsActive +
            '}';
    }
}
