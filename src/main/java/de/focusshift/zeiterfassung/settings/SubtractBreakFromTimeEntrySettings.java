package de.focusshift.zeiterfassung.settings;

import java.time.Instant;

/**
 * Global setting for behaviour for subtraction of breaks of time entries.
 *
 * <p>
 * Example for {@link SubtractBreakFromTimeEntrySettings#subtractBreakFromTimeEntryIsActive}:
 * <ul>
 *     <li>{@code false} - breaks are not subtracted from time entries</li>
 *     <li>{@code true} - breaks are subctracted form time entries</li>
 * </ul>
 *
 * <p>
 *
 * @param subtractBreakFromTimeEntryIsActive whether this feature is enabled or not
 */
public record SubtractBreakFromTimeEntrySettings(boolean subtractBreakFromTimeEntryIsActive, Instant subtractBreakFromTimeEntryEnabledTimestamp) {

    /**
     * Default LockTimeSettings.
     *
     * <ul>
     *     <li>{@link SubtractBreakFromTimeEntrySettings#subtractBreakFromTimeEntryIsActive()} = {@code true}</li>
     * </ul>
     */
    public static final SubtractBreakFromTimeEntrySettings DEFAULT = new SubtractBreakFromTimeEntrySettings(true, Instant.now());
}
