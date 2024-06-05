package de.focusshift.zeiterfassung.importer.model;


public record WorkingTimeDTO(WorkDayDTO monday, WorkDayDTO tuesday, WorkDayDTO wednesday, WorkDayDTO thursday,
                             WorkDayDTO friday, WorkDayDTO saturday, WorkDayDTO sunday) {
}
