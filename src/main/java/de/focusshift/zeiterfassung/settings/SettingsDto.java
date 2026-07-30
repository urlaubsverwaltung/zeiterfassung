package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import jakarta.annotation.Nullable;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

record SettingsDto(

    FederalState federalState,
    boolean worksOnPublicHoliday,

    boolean lockingIsActive,
    @Nullable
    String lockTimeEntriesDaysInPast,

    @Nullable
    Boolean subtractBreakFromTimeEntryIsActive,
    @Nullable
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate subtractBreakFromTimeEntryActiveDate,

    boolean commentEnabled,
    boolean breakIntegrated,
    @Nullable
    String defaultBreakMinutes
) {

    /**
     * Returns the user input string as number, or {@code null} when there is no value, or it is not a number.
     *
     * @return number value of the user input
     */
    @Nullable
    public Integer lockTimeEntriesDaysInPastAsNumber() {
        return parseNumber(lockTimeEntriesDaysInPast);
    }

    /**
     * Returns the user input string as number, or {@code null} when there is no value, or it is not a number.
     *
     * @return number value of the user input
     */
    @Nullable
    public Integer defaultBreakMinutesAsNumber() {
        return parseNumber(defaultBreakMinutes);
    }

    @Nullable
    private static Integer parseNumber(@Nullable String value) {
        if (value == null) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // ignore it, has to be covered by bean validation if necessary
            return null;
        }
    }
}
