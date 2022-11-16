package de.focusshift.zeiterfassung.report;

import java.time.YearMonth;
import java.util.List;

record ReportMonth(YearMonth yearMonth, List<ReportWeek> weeks) {
}
