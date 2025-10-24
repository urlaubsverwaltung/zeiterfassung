package de.focusshift.zeiterfassung.settings;

import java.time.Instant;

/**
 * Global setting for behaviour for subtraction of breaks of time entries.
 *
 * <p>
 * Example for {@link SubtractBreakFromTimeEntrySettings#subtractBreakFromTimeEntryIsActive}:
 * <ul>
 *     <li>{@code false} - breaks are not subtracted from time entries</li>
 *     <li>{@code true} - breaks are subtracted form time entries</li>
 * </ul>
 *
 * <p>
 *
 * @param subtractBreakFromTimeEntryIsActive whether this feature is enabled or not
 * @param subtractBreakFromTimeEntryEnabledTimestamp timestamp of feature activation
 */
public record SubtractBreakFromTimeEntrySettings(boolean subtractBreakFromTimeEntryIsActive, Instant subtractBreakFromTimeEntryEnabledTimestamp) {
}
