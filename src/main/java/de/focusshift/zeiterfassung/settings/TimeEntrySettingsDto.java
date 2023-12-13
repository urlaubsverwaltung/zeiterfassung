package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.timeentry.settings.TimeEntryFreeze;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

@Validated
class TimeEntrySettingsDto {

    private boolean enabled;
    @PositiveOrZero
    private int value;
    @NotNull
    private TimeEntryFreeze.Unit unit;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public TimeEntryFreeze.Unit getUnit() {
        return unit;
    }

    public void setUnit(TimeEntryFreeze.Unit unit) {
        this.unit = unit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeEntrySettingsDto that = (TimeEntrySettingsDto) o;
        return enabled == that.enabled && value == that.value && Objects.equals(unit, that.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, value, unit);
    }

    @Override
    public String toString() {
        return "TimeEntrySettingsDto{" +
            "enabled=" + enabled +
            ", value=" + value +
            ", unit='" + unit + '\'' +
            '}';
    }
}
