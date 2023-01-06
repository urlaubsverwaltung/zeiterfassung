package de.focusshift.zeiterfassung.report;

import java.util.List;

record DetailWeekDto(String yearMonthWeek, List<DetailDayDto> dayReports) {

}
