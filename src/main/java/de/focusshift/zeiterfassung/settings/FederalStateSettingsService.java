package de.focusshift.zeiterfassung.settings;

public interface FederalStateSettingsService {

    /**
     * Returns the persisted {@link FederalStateSettings} or a default when nothing has been configured yet,
     * never {@code null}.
     *
     * @return the {@link FederalStateSettings}
     */
    FederalStateSettings getFederalStateSettings();
}
