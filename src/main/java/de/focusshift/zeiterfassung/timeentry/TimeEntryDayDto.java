package de.focusshift.zeiterfassung.timeentry;

import java.util.List;

record TimeEntryDayDto(String date,
                       String hoursWorked,
                       List<TimeEntryDTO> timeEntries) {
}
