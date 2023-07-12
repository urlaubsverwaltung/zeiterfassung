package de.focusshift.zeiterfassung.report;

import java.util.List;

record DetailDayDto(boolean differentMonth, String dayOfWeek, String dayOfWeekFull, String date, Double hoursWorked, List<DetailDayEntryDto> dayEntries, List<DetailDayAbsenceDto> absences) {
}
