package de.focusshift.zeiterfassung.timeentry;

import java.util.List;

record TimeEntryWeekDto(Integer calendarWeek, String from, String to, String hoursWorked,
                        List<TimeEntryDTO> timeEntries) {
}
