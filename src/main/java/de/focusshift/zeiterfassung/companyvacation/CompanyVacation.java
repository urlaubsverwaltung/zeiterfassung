package de.focusshift.zeiterfassung.companyvacation;

import java.time.Instant;

public record CompanyVacation(
    Instant startDate,
    Instant endDate,
    DayLength dayLength
) {
}
