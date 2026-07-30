package de.focusshift.zeiterfassung.settings;

/**
 * Global settings for the time entry form and its behaviour.
 *
 * @param commentEnabled       whether the comment input is shown on the time entry form
 * @param breakIntegrated      whether break is captured as a duration on the time entry row itself
 *                             instead of a separate break row
 * @param defaultBreakMinutes  break duration in minutes used to pre-populate a new time entry row
 *                             when {@link TimeEntrySettings#breakIntegrated} is {@code true}
 */
public record TimeEntrySettings(boolean commentEnabled, boolean breakIntegrated, int defaultBreakMinutes) {

    public static final TimeEntrySettings DEFAULT = new TimeEntrySettings(true, false, 45);
}
