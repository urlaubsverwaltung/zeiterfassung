package de.focusshift.zeiterfassung.timeentry.republish;

import de.focusshift.zeiterfassung.timeentry.events.DayLockedEvent;

import java.time.LocalDate;

interface DayLockedRepublishService {

    /**
     * Republishes {@link DayLockedEvent} for every active tenant, for each day in the
     * inclusive range {@code [from, to]}.
     *
     * @param from first day to republish (inclusive)
     * @param to   last day to republish (inclusive)
     */
    void republishDayLockedEvents(LocalDate from, LocalDate to);

    /**
     * Republishes {@link DayLockedEvent} for the given tenant only, for each day in the
     * inclusive range {@code [from, to]}.
     *
     * @param tenantId tenant to republish for
     * @param from     first day to republish (inclusive)
     * @param to       last day to republish (inclusive)
     */
    void republishDayLockedEvents(String tenantId, LocalDate from, LocalDate to);
}
