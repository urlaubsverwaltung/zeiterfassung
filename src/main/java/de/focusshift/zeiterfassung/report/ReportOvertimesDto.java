package de.focusshift.zeiterfassung.report;

import java.time.LocalDate;
import java.util.List;

record ReportOvertimesDto(List<LocalDate> dayOfWeeks, List<ReportOvertimeDto> overtimes) {
}
