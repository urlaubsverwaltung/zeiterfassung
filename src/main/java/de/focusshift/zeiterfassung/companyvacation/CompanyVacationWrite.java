package de.focusshift.zeiterfassung.companyvacation;

import java.time.Instant;

/**
 * Describes the write model of a company vacation like noon at Christmas Eve.
 *
 * @param sourceId
 * @param startDate
 * @param endDate
 * @param dayLength
 */
public record CompanyVacationWrite(String sourceId, Instant startDate, Instant endDate, DayLength dayLength) {
}
