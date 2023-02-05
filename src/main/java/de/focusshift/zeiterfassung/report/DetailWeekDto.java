package de.focusshift.zeiterfassung.report;

import java.util.Date;
import java.util.List;

record DetailWeekDto(Date firstDateOfWeek, Date lastDateOfWeek, int calendarWeek, List<DetailDayDto> dayReports) {

}
