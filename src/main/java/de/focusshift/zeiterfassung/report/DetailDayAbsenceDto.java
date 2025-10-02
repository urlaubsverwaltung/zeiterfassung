package de.focusshift.zeiterfassung.report;

record DetailDayAbsenceDto(
    String username,
    String initials,
    Long userLocalId,
    String dayLength,
    String name,
    String color
) {
}
