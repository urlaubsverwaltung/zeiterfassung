package de.focusshift.zeiterfassung.settings;

public interface OooCalendarSettingsService {

    /**
     * Returns the persisted {@link OooCalendarSettings}, or the default (unconfigured) instance if not set.
     *
     * @return the {@link OooCalendarSettings}, never {@code null}
     */
    OooCalendarSettings getOooCalendarSettings();
}
