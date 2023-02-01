package de.focusshift.zeiterfassung.report;

import java.util.List;

record DetailMonthDto(String yearMonth, List<DetailWeekDto> weekReports) {
}
