package de.focusshift.zeiterfassung.report;

import jakarta.annotation.Nullable;

import java.util.List;

record DetailDayDto(
    boolean differentMonth,
    String dayOfWeek,
    String dayOfWeekFull,
    String date,
    boolean locked,
    String workedWorkingHours,
    String shouldWorkingHours,
    String hoursDelta,
    boolean hoursDeltaNegative,
    List<DetailDayEntryDto> dayEntries,
    List<DetailDayAbsenceDto> absences,
    @Nullable String breakDeduction,
    @Nullable String breakViolationMessageKey
) {

    DetailDayDto(boolean differentMonth, String dayOfWeek, String dayOfWeekFull, String date, boolean locked,
                 String workedWorkingHours, String shouldWorkingHours, String hoursDelta, boolean hoursDeltaNegative,
                 List<DetailDayEntryDto> dayEntries, List<DetailDayAbsenceDto> absences) {
        this(differentMonth, dayOfWeek, dayOfWeekFull, date, locked, workedWorkingHours, shouldWorkingHours, hoursDelta,
            hoursDeltaNegative, dayEntries, absences, null, null);
    }
}
