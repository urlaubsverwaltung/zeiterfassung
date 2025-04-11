package de.focusshift.zeiterfassung.report;

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
    List<DetailDayAbsenceDto> absences
) {
}
