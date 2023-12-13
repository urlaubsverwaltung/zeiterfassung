package de.focusshift.zeiterfassung.timeentry.settings;

public interface TimeEntrySettingsService {

    TimeEntrySettings getTimeEntrySettings();

    TimeEntrySettings updateTimeEntrySettings(TimeEntryFreeze timeEntryFreeze);
}
