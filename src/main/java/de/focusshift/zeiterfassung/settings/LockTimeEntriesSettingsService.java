package de.focusshift.zeiterfassung.settings;

public interface LockTimeEntriesSettingsService {

    /**
     * Returns the persisted {@link LockTimeEntriesSettings} or a default when nothing has been configured yet,
     * never {@code null}.
     *
     * @return the {@link LockTimeEntriesSettings}
     */
    LockTimeEntriesSettings getLockTimeEntriesSettings();
}
