package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.user.UserId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AbsenceService {

    /**
     * Fina all absences for teh given criteria
     *
     * @param userId user
     * @param from from
     * @param toExclusive to (exclusive)
     * @return absences grouped by date. empty list value when there are no absences on a date within the period.
     */
    Map<LocalDate, List<Absence>> findAllAbsences(UserId userId, Instant from, Instant toExclusive);
}
