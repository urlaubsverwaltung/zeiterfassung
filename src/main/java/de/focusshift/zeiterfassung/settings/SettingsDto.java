package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import jakarta.annotation.Nullable;

record SettingsDto(
    FederalState federalState,
    boolean worksOnPublicHoliday,
    boolean lockingIsActive,
    @Nullable
    String lockTimeEntriesDaysInPast
) {

    /**
     * Returns the user input string as number, or {@code null} when there is no value, or it is not a number.
     *
     * @return number value of the user input
     */
    @Nullable
    public Integer lockTimeEntriesDaysInPastAsNumber() {
        if (lockTimeEntriesDaysInPast == null) {
            return null;
        }

        try {
            return Integer.parseInt(lockTimeEntriesDaysInPast);
        } catch (NumberFormatException e) {
            // ignore it, has to be covered by bean validation if necessary
            return null;
        }
    }
}
