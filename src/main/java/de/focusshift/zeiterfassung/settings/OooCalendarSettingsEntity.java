package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import java.util.Objects;

@Entity(name = "settings_ooo_calendar")
public class OooCalendarSettingsEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "settings_ooo_calendar_seq", sequenceName = "settings_ooo_calendar_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "settings_ooo_calendar_seq")
    protected Long id;

    @Nullable
    @Column(name = "calendar_url", length = 2048)
    private String calendarUrl;

    protected OooCalendarSettingsEntity() {
        super(null);
    }

    public Long getId() {
        return id;
    }

    @Nullable
    public String getCalendarUrl() {
        return calendarUrl;
    }

    public void setCalendarUrl(@Nullable String calendarUrl) {
        this.calendarUrl = calendarUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OooCalendarSettingsEntity that = (OooCalendarSettingsEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
