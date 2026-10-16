package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

@Entity(name = "settings_time_entry")
public class TimeEntrySettingsEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "settings_time_entry_seq", sequenceName = "settings_time_entry_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "settings_time_entry_seq")
    protected Long id;

    @NotNull
    @Column(name = "comment_enabled", nullable = false)
    private boolean commentEnabled;

    @NotNull
    @Column(name = "duration_enabled", nullable = false)
    private boolean durationEnabled;

    @NotNull
    @Column(name = "break_enabled", nullable = false)
    private boolean breakEnabled;

    protected TimeEntrySettingsEntity() {
        super(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isCommentEnabled() {
        return commentEnabled;
    }

    public void setCommentEnabled(boolean commentEnabled) {
        this.commentEnabled = commentEnabled;
    }

    public boolean isDurationEnabled() {
        return durationEnabled;
    }

    public void setDurationEnabled(boolean durationEnabled) {
        this.durationEnabled = durationEnabled;
    }

    public boolean isBreakEnabled() {
        return breakEnabled;
    }

    public void setBreakEnabled(boolean breakEnabled) {
        this.breakEnabled = breakEnabled;
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
            ", commentEnabled=" + commentEnabled +
            ", durationEnabled=" + durationEnabled +
            ", breakEnabled=" + breakEnabled +
            '}';
    }
}
