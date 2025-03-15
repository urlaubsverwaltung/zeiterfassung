package de.focusshift.zeiterfassung.report;

record DetailDayAbsenceDto(
    String username,
    Long userLocalId,
    String dayLength,
    String name,
    String color
) {
}
