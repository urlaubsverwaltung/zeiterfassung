package de.focusshift.zeiterfassung.settings;

public interface WorkingTimeSettingsService {

    /**
     * Returns the persisted {@link WorkingTimeSettings} or the default (Mon–Fri 8h) when nothing
     * has been configured yet, never {@code null}.
     *
     * @return the {@link WorkingTimeSettings}
     */
    WorkingTimeSettings getWorkingTimeSettings();
}
