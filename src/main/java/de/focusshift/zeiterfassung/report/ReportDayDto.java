package de.focusshift.zeiterfassung.report;

record ReportDayDto(boolean differentMonth, String dayOfWeek, String dayOfWeekFull, String date, Double hoursWorked) {
}
