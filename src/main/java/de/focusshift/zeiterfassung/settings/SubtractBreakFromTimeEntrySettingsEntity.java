package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;

@Entity(name = "settings_subtract_break_from_time_entry")
public class SubtractBreakFromTimeEntrySettingsEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "settings_subtract_break_from_time_entry_seq", sequenceName = "settings_subtract_break_from_time_entry_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "settings_subtract_break_from_time_entry_seq")
    protected Long id;

    @NotNull
    @Column(name = "enabled", nullable = false)
    private boolean subtractBreakFromTimeEntryIsActive;

    @Column(name = "enabled_timestamp")
    private Instant subtractBreakFromTimeEntryEnabledTimestamp;

    protected SubtractBreakFromTimeEntrySettingsEntity() {
        super(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isSubtractBreakFromTimeEntryIsActive() {
        return subtractBreakFromTimeEntryIsActive;
    }

    public void setSubtractBreakFromTimeEntryIsActive(boolean subtractBreakFromTimeEntryIsActive) {
        this.subtractBreakFromTimeEntryIsActive = subtractBreakFromTimeEntryIsActive;
    }

    public Instant getSubtractBreakFromTimeEntryEnabledTimestamp() {
        return subtractBreakFromTimeEntryEnabledTimestamp;
    }

    public void setSubtractBreakFromTimeEntryEnabledTimestamp(Instant subtractBreakFromTimeEntryEnabledTimestamp) {
        this.subtractBreakFromTimeEntryEnabledTimestamp = subtractBreakFromTimeEntryEnabledTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        SubtractBreakFromTimeEntrySettingsEntity that = (SubtractBreakFromTimeEntrySettingsEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "SubtractBreakFromTimeEntrySettingsEntity{" +
            "id=" + id +
            ", subtractBreakFromTimeEntryIsActive=" + subtractBreakFromTimeEntryIsActive +
            ", subtractBreakFromTimeEntryEnabledTimestamp=" + subtractBreakFromTimeEntryEnabledTimestamp +
            '}';
    }
}
