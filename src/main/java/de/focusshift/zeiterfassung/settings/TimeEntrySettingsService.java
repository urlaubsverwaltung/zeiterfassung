package de.focusshift.zeiterfassung.settings;

public interface TimeEntrySettingsService {

    /**
     * Returns the persisted {@link TimeEntrySettings} or a default when nothing has been configured yet,
     * never {@code null}.
     *
     * @return the {@link TimeEntrySettings}
     */
    TimeEntrySettings getTimeEntrySettings();
}
