package de.focusshift.zeiterfassung.timeentry;

record TimeEntryWeeksPageDto(int futureYear, int futureWeekOfYear, int pastYear, int pastWeekOfYear,
                             TimeEntryWeekDto timeEntryWeek, long totalTimeEntryElements) {
}
