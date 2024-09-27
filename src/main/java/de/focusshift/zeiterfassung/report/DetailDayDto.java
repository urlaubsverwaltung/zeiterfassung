package de.focusshift.zeiterfassung.report;

import java.time.Duration;
import java.util.List;

record DetailDayDto(
    boolean differentMonth,
    String dayOfWeek,
    String dayOfWeekFull,
    String date,
    Duration hoursWorked,
    List<DetailDayEntryDto> dayEntries,
    List<DetailDayAbsenceDto> absences
) {
}
