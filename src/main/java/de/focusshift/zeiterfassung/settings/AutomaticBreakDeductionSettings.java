package de.focusshift.zeiterfassung.settings;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

public record AutomaticBreakDeductionSettings(boolean active, Optional<Instant> enabledTimestamp) {

    public Optional<LocalDate> enabledDate(ZoneId zoneId) {
        return enabledTimestamp.map(t -> t.atZone(zoneId).toLocalDate());
    }
}
