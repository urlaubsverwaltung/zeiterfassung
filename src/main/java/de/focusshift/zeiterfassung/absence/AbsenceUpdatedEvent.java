package de.focusshift.zeiterfassung.absence;

// TODO: Own Dtos?
public record AbsenceUpdatedEvent(AbsenceWrite newAbsence,
                                  AbsenceWriteEntity existingAbsence) {
}
