package de.focusshift.zeiterfassung.settings;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Settings concerning the statutory breaks of ArbZG §4.
 *
 * @param active               whether the missing part of the mandatory break is deducted from the work duration
 * @param enabledTimestamp     start of the first day the deduction applies to
 * @param violationHintsActive whether hints about violated break rules are shown, independent of {@code active}
 */
public record AutomaticBreakDeductionSettings(
    boolean active,
    Optional<Instant> enabledTimestamp,
    boolean violationHintsActive
) {

    public static final AutomaticBreakDeductionSettings DISABLED =
        new AutomaticBreakDeductionSettings(false, Optional.empty(), false);

    public Optional<LocalDate> enabledDate(ZoneId zoneId) {
        return enabledTimestamp.map(t -> t.atZone(zoneId).toLocalDate());
    }

    /**
     * @param date   the day to check
     * @param zoneId zone to interpret the {@link #enabledTimestamp()} in
     * @return whether the mandatory break is deducted for the given day
     */
    public boolean isDeductionActiveOn(LocalDate date, ZoneId zoneId) {
        return active && enabledDate(zoneId).map(enabledDate -> !date.isBefore(enabledDate)).orElse(false);
    }
}
