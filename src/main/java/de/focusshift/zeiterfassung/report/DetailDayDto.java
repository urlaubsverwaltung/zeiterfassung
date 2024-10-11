package de.focusshift.zeiterfassung.report;

import java.time.Duration;
import java.util.List;

record DetailDayDto(
    boolean differentMonth,
    String dayOfWeek,
    String dayOfWeekFull,
    String date,
    Duration hoursWorked, /* todo remove maybe? */
    String workedWorkingHours,
    String shouldWorkingHours,
    String hoursDelta,
    boolean hoursDeltaNegative,
    List<DetailDayEntryDto> dayEntries,
    List<DetailDayAbsenceDto> absences
) {
}
