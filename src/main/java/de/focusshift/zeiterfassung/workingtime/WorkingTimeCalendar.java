package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.AbsenceTypeCategory;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.usermanagement.User;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.apache.commons.lang3.compare.ComparableUtils.max;

/**
 * Provides information about {@link PlannedWorkingHours} / {@link ShouldWorkingHours} on a given {@link LocalDate}.
 * (including publicHolidays and {@linkplain Absence absences})
 *
 * <p>
 * For instance:
 *
 * <ul>
 *     <li>2022-12-26 - 0h (publicHoliday, monday)</li>
 *     <li>2022-12-27 - 8h (tuesday)</li>
 *     <li>2022-12-28 - 4h (wednesday, absent on noon)</li>
 *     <li>2022-12-29 - 8h (thursday)</li>
 *     <li>2022-12-30 - 4h (friday)</li>
 * </ul>
 * <p>
 * Should be used in combination with a {@link Map} to keep relation to a {@link User} for example.
 */
public final class WorkingTimeCalendar {

    private final Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate;
    private final Map<LocalDate, List<Absence>> absencesByDate;

    public WorkingTimeCalendar(Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate, Map<LocalDate, List<Absence>> absencesByDate) {
        this.plannedWorkingHoursByDate = plannedWorkingHoursByDate;
        this.absencesByDate = absencesByDate;
    }

    /**
     * Returns the {@link PlannedWorkingHours} for the given date or empty when the date is unknown in this calendar.
     *
     * @param date date to get the {@link PlannedWorkingHours} for
     * @return the {@link PlannedWorkingHours} for the given date or empty when the date is unknown in this calendar.
     */
    public Optional<PlannedWorkingHours> plannedWorkingHours(LocalDate date) {
        return Optional.ofNullable(plannedWorkingHoursByDate.get(date));
    }

    /**
     * Returns the {@link ShouldWorkingHours} for the given date or empty when the date is unknown in this calendar.
     *
     * @param date date to get the {@link ShouldWorkingHours} for
     * @return the {@link ShouldWorkingHours} for the given date or empty when the date is unknown in this calendar.
     */
    public Optional<ShouldWorkingHours> shouldWorkingHours(LocalDate date) {

        final PlannedWorkingHours plannedWorkingHours = plannedWorkingHoursByDate.get(date);
        if (plannedWorkingHours == null) {
            return Optional.empty();
        }

        final List<Absence> absences = absencesByDate.getOrDefault(date, List.of());
        Duration absenceDuration = Duration.ZERO;
        final Duration plannedWorkingHourDuration = plannedWorkingHours.duration();

        for (Absence absence : absences) {
            if (absence.absenceTypeCategory() == AbsenceTypeCategory.OVERTIME) {
                absenceDuration = absenceDuration.plus(absence.overtimeHours());
            } else {
                // application for leave or sicknote
                double absenceValue = absence.dayLength().getValue();

                if (absenceValue == 0.5) {
                    // half day absence -> should == planned / 2
                    absenceDuration = absenceDuration.plus(plannedWorkingHourDuration.dividedBy(2));
                } else if (absenceValue == 1.0) {
                    // full day absence -> should == ZERO
                    absenceDuration = plannedWorkingHourDuration;
                }
            }
        }

        return Optional.of(new ShouldWorkingHours(max(plannedWorkingHours.duration().minus(absenceDuration), Duration.ZERO)));
    }

    /**
     * Returns list of absences for the given date. Can have length of:
     *
     * <ul>
     *     <li>{@code zero}: no absences</li>
     *     <li>{@code one}: one full- or one half-day-absence</li>
     *     <li>{@code two}: two half-day-absences</li>
     * </ul>
     *
     * @param date date to get the {@link Absence}s for
     * @return list of absences for the given date
     */
    public Optional<List<Absence>> absence(LocalDate date) {
        return Optional.ofNullable(absencesByDate.get(date));
    }

    /**
     * calculate {@linkplain PlannedWorkingHours} between the given dates.
     */
    public PlannedWorkingHours plannedWorkingHours(LocalDate from, LocalDate toExclusive) {
        return plannedWorkingHoursByDate.entrySet()
            .stream()
            .filter(entry -> isDateBetween(entry.getKey(), from, toExclusive))
            .map(Map.Entry::getValue)
            .reduce(PlannedWorkingHours.ZERO, PlannedWorkingHours::plus);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkingTimeCalendar that = (WorkingTimeCalendar) o;
        return Objects.equals(plannedWorkingHoursByDate, that.plannedWorkingHoursByDate) && Objects.equals(absencesByDate, that.absencesByDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plannedWorkingHoursByDate, absencesByDate);
    }

    @Override
    public String toString() {
        return "WorkingTimeCalendar{" +
            "plannedWorkingHoursByDate=" + plannedWorkingHoursByDate +
            ", absencesByDate=" + absencesByDate +
            '}';
    }

    private boolean isDateBetween(LocalDate toCheck, LocalDate from, LocalDate toExclusive) {
        return toCheck.isBefore(toExclusive) && (toCheck.isEqual(from) || toCheck.isAfter(from));
    }
}
