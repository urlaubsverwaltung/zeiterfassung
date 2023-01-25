package de.focusshift.zeiterfassung.timeentry;

import java.util.Collection;
import java.util.List;

record TimeEntryWeekDto(Integer calendarWeek, String from, String to, String hoursWorked, List<TimeEntryDayDto> days) {
    public List<TimeEntryDTO> timeEntries() {
        return days.stream().map(TimeEntryDayDto::timeEntries).flatMap(Collection::stream).toList();
    }
}
