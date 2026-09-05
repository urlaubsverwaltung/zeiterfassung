package de.focusshift.zeiterfassung.settings;

public record TimeEntrySettings(boolean commentEnabled, boolean durationEnabled, boolean breakEnabled) {

    public static final TimeEntrySettings DEFAULT = new TimeEntrySettings(true, true, true);
}
