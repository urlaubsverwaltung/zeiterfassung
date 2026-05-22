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

@Entity(name = "settings_automatic_break_deduction")
class AutomaticBreakDeductionSettingsEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "settings_automatic_break_deduction_seq", sequenceName = "settings_automatic_break_deduction_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "settings_automatic_break_deduction_seq")
    private Long id;

    @NotNull
    @Column(name = "enabled", nullable = false)
    private boolean active;

    @Column(name = "enabled_timestamp")
    private Instant enabledTimestamp;

    protected AutomaticBreakDeductionSettingsEntity() {
        super(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getEnabledTimestamp() {
        return enabledTimestamp;
    }

    public void setEnabledTimestamp(Instant enabledTimestamp) {
        this.enabledTimestamp = enabledTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AutomaticBreakDeductionSettingsEntity that = (AutomaticBreakDeductionSettingsEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
