package de.focusshift.zeiterfassung.importer.model;

import java.util.List;

public record UserExport(UserDTO user, OvertimeAccountDTO overtimeAccount, WorkingTimeDTO workingTime,
                         List<TimeClockDTO> timeClocks, List<TimeEntryDTO> timeEntries) {
}


