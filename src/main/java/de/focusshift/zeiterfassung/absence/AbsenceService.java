package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AbsenceService {

    /**
     * Find all absences for the given criteria
     *
     * @param userId      user
     * @param from        from
     * @param toExclusive to (exclusive)
     * @return absences grouped by date. empty list value when there are no absences on a date within the period.
     */
    Map<LocalDate, List<Absence>> findAllAbsences(UserId userId, Instant from, Instant toExclusive);

    /**
     * Find all absences in a given date range for the given users
     *
     * @param from        from
     * @param toExclusive to (exclusive)
     * @return absences grouped by {@link UserLocalId}. empty list value when there are no absences on a date within the period.
     */
    Map<UserLocalId, List<Absence>> getAbsencesForAllUsers(LocalDate from, LocalDate toExclusive);

    /**
     * Find all absences in a given date range for the given users
     *
     * @param userLocalIds users
     * @param from         from
     * @param toExclusive  to (exclusive)
     * @return absences grouped by {@link UserLocalId}. empty list value when there are no absences on a date within the period.
     */
    Map<UserLocalId, List<Absence>> getAbsencesByUserIds(List<UserLocalId> userLocalIds, LocalDate from, LocalDate toExclusive);

    /**
     * Find all absences in a given date range for the given user
     *
     * @param userId      user
     * @param from        from
     * @param toExclusive to (exclusive)
     * @return list of absences of {@link UserId}. empty list value when there are no absences on a date within the period.
     */
    List<Absence> getAbsencesByUserId(UserId userId, LocalDate from, LocalDate toExclusive);
}
