package de.focusshift.zeiterfassung.timeentry;

record TimeEntryWeeksPageDto(int futureYear, long futureWeekOfYear, int pastYear, long pastWeekOfYear, TimeEntryWeekDto timeEntryWeek, long totalTimeEntryElements) {
}
