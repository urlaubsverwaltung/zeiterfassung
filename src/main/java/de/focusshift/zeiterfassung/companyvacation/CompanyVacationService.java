package de.focusshift.zeiterfassung.companyvacation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface CompanyVacationService {

    /**
     * Find all company vacations in a given date range
     *
     * @param from        from
     * @param toExclusive to (exclusive)
     * @return company vacations. empty list value when there are no company vacations on a date within the period.
     */
    List<CompanyVacation> getCompanyVacations(Instant from, Instant toExclusive);
}
