package de.focusshift.zeiterfassung.settings;

import java.time.Instant;

public record SubtractBreakFromTimeEntrySettingsDto(boolean subtractBreakFromTimeEntry, Instant subtractBreakFromTimeEntryEnabledTimestamp) {
}
