package de.focusshift.zeiterfassung.timeentry.settings;

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

@Entity(name = "time_entry_settings")
public class TimeEntrySettingsEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "time_entry_settings_seq", sequenceName = "time_entry_settings_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "time_entry_settings_seq")
    protected Long id;

    @Column(name = "time_entry_freeze_enabled")
    private boolean timeEntryFreezeEnabled;

    @Column(name = "time_entry_freeze_value")
    private int timeEntryFreezeValue;

    @Column(name = "time_entry_freeze_unit")
    @Enumerated(STRING)
    private TimeEntryFreeze.Unit timeEntryFreezeUnit;

    protected TimeEntrySettingsEntity(String tenantId, Long id, int timeEntryFreezeValue, TimeEntryFreeze.Unit timeEntryFreezeUnit) {
        super(tenantId);
        this.id = id;
        this.timeEntryFreezeValue = timeEntryFreezeValue;
        this.timeEntryFreezeUnit = timeEntryFreezeUnit;
    }

    protected TimeEntrySettingsEntity() {
        super(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isTimeEntryFreezeEnabled() {
        return timeEntryFreezeEnabled;
    }

    public void setTimeEntryFreezeEnabled(boolean timeEntryFreezeEnabled) {
        this.timeEntryFreezeEnabled = timeEntryFreezeEnabled;
    }

    public int getTimeEntryFreezeValue() {
        return timeEntryFreezeValue;
    }

    public void setTimeEntryFreezeValue(int timeEntryFreezeValue) {
        this.timeEntryFreezeValue = timeEntryFreezeValue;
    }

    public TimeEntryFreeze.Unit getTimeEntryFreezeUnit() {
        return timeEntryFreezeUnit;
    }

    public void setTimeEntryFreezeUnit(TimeEntryFreeze.Unit timeEntryFreezeUnit) {
        this.timeEntryFreezeUnit = timeEntryFreezeUnit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeEntrySettingsEntity that = (TimeEntrySettingsEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TimeEntrySettingsEntity{" +
            "id=" + id +
            ", timeEntryFreezeEnabled=" + timeEntryFreezeEnabled +
            ", timeEntryFreezeValue=" + timeEntryFreezeValue +
            ", timeEntryFreezeUnit=" + timeEntryFreezeUnit +
            '}';
    }
}
