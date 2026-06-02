package de.focusshift.zeiterfassung.suggestion;

/**
 * A suggested time entry derived from a Urlaubsverwaltung absence event.
 *
 * @param summary     the event summary, e.g. "Vacation"
 * @param date        ISO date string (yyyy-MM-dd)
 * @param description additional description, empty for absence events
 */
public record CalendarSuggestionDto(
    String summary,
    String date,
    String description
) {
}
