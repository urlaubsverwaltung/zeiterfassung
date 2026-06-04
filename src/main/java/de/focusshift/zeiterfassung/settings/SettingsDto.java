package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import jakarta.annotation.Nullable;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

record SettingsDto(

    FederalState federalState,
    boolean worksOnPublicHoliday,

    /** Checked day names: "monday", "tuesday", … */
    @Nullable List<String> workday,
    /** Common hours per working day (decimal, e.g. 8.0 or 7.5). */
    @Nullable Double workingTime,
    /** Round suggested GitHub Activity time up to the nearest multiple of this many minutes (1–60). */
    @Nullable Integer timeRoundingMinutes,
    /** Minimum floor for the GitHub Activity time suggestion in minutes (1–480). */
    @Nullable Integer minSuggestedMinutes,

    /** When {@code true} users must select a project on every time entry. */
    @Nullable Boolean projectRequired,
    /** When {@code true} users must select an activity type on every time entry. */
    @Nullable Boolean activityTypeRequired,

    boolean lockingIsActive,
    @Nullable
    String lockTimeEntriesDaysInPast,

    @Nullable
    Boolean subtractBreakFromTimeEntryIsActive,
    @Nullable
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate subtractBreakFromTimeEntryActiveDate,

    @Nullable
    String oooCalendarUrl
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
