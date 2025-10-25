package de.focusshift.zeiterfassung.settings;

import java.util.Optional;

public interface SubtractBreakFromTimeEntrySettingsService {

    /**
     * Returns the persisted {@link SubtractBreakFromTimeEntrySettings} or a default when nothing has been configured yet,
     * never {@code null}.
     *
     * @return the {@link SubtractBreakFromTimeEntrySettings}
     */
    Optional<SubtractBreakFromTimeEntrySettings> getSubtractBreakFromTimeEntrySettings();
}
