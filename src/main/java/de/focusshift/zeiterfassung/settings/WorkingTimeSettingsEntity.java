package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import java.util.Objects;

@Entity(name = "settings_working_time")
class WorkingTimeSettingsEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "settings_working_time_seq", sequenceName = "settings_working_time_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "settings_working_time_seq")
    protected Long id;

    @Column(name = "monday",    nullable = false) private String monday    = "PT8H";
    @Column(name = "tuesday",   nullable = false) private String tuesday   = "PT8H";
    @Column(name = "wednesday", nullable = false) private String wednesday = "PT8H";
    @Column(name = "thursday",  nullable = false) private String thursday  = "PT8H";
    @Column(name = "friday",    nullable = false) private String friday    = "PT8H";
    @Column(name = "saturday",  nullable = false) private String saturday  = "PT0S";
    @Column(name = "sunday",    nullable = false) private String sunday    = "PT0S";

    WorkingTimeSettingsEntity() {
        super(null);
    }

    public Long getId() { return id; }

    public String getMonday()    { return monday; }
    public void setMonday(String monday)       { this.monday    = monday; }

    public String getTuesday()   { return tuesday; }
    public void setTuesday(String tuesday)     { this.tuesday   = tuesday; }

    public String getWednesday() { return wednesday; }
    public void setWednesday(String wednesday) { this.wednesday = wednesday; }

    public String getThursday()  { return thursday; }
    public void setThursday(String thursday)   { this.thursday  = thursday; }

    public String getFriday()    { return friday; }
    public void setFriday(String friday)       { this.friday    = friday; }

    public String getSaturday()  { return saturday; }
    public void setSaturday(String saturday)   { this.saturday  = saturday; }

    public String getSunday()    { return sunday; }
    public void setSunday(String sunday)       { this.sunday    = sunday; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkingTimeSettingsEntity that = (WorkingTimeSettingsEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
