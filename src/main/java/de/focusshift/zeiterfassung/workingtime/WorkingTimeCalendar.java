package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.usermanagement.User;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.OVERTIME;
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

        final List<Absence> absencesAtGivenDate = absencesByDate.getOrDefault(date, List.of());
        Duration absenceDuration = Duration.ZERO;
        final Duration plannedWorkingHourDuration = plannedWorkingHours.duration();

        for (Absence absence : absencesAtGivenDate) {
            if (absence.absenceTypeCategory() == OVERTIME) {
                final Duration duration = getAbsenceDurationOfOvertimeReduction(absence, absencesAtGivenDate);
                absenceDuration = absenceDuration.plus(duration);
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
        final List<Absence> absences = absencesByDate.get(date);
        if (absences == null) {
            return Optional.empty();
        }

        // overtime-reduction is an exceptional case of an absence
        // which is only of interest when the person has to work at the given date.
        final boolean hasNotToWorkAtDate = getEffectiveWorkingDays(date, date) == 0;
        if (hasNotToWorkAtDate) {
            return Optional.of(
                absences.stream()
                    .filter(a -> !a.absenceTypeCategory().equals(OVERTIME))
                    .toList()
            );
        }

        return Optional.of(absences);
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

    /**
     * Calculate the duration of the overtime reduction absence.
     *
     * @param overtimeReductionAbsence overtime reduction absence
     * @param otherAbsences all other absences at a certain date
     * @return duration of the overtime reduction absence
     */
    private Duration getAbsenceDurationOfOvertimeReduction(Absence overtimeReductionAbsence, List<Absence> otherAbsences) {
        final LocalDate startDate = overtimeReductionAbsence.startDate().atZone(ZoneId.of("UTC")).toLocalDate();
        final LocalDate endDate = overtimeReductionAbsence.endDate().atZone(ZoneId.of("UTC")).toLocalDate();

        // Calculate effective working days considering other absences
        // overtime reduction hours are only distributed over days that are no absent days. (e.g. companyVacation)
        final double effectiveWorkingDays = getEffectiveWorkingDays(startDate, endDate);
        if (effectiveWorkingDays > 0) {
            // For the given date (otherAbsences), calculate its capacity to determine the portion of overtime
            final double capacity = getShouldWorkDayLengthCapacity(otherAbsences);
            // Calculate overtime for this specific day based on its capacity
            final Duration overtimeHoursPerFullDay = Duration.ofMillis((long)(overtimeReductionAbsence.overtimeHours().toMillis() / effectiveWorkingDays));
            return Duration.ofMillis((long)(overtimeHoursPerFullDay.toMillis() * capacity));
        }

        return Duration.ZERO;
    }

    /**
     * Calculates the dayLength value that should be worked for the given interval.
     *
     * @param startDate start date
     * @param endDate end date
     * @return the dayLength value that should be worked in the interval (0, 0.5, 1, 1.5, ...)
     */
    private double getEffectiveWorkingDays(LocalDate startDate, LocalDate endDate) {
        double effectiveWorkingDays = 0.0;

        for (LocalDate dayInAbsence = startDate; !dayInAbsence.isAfter(endDate); dayInAbsence = dayInAbsence.plusDays(1)) {
            final PlannedWorkingHours dayPlanned = plannedWorkingHoursByDate.getOrDefault(dayInAbsence, PlannedWorkingHours.ZERO);
            if (dayPlanned.duration().isPositive()) {
                // Check for other absences on this day
                final List<Absence> dayAbsences = absencesByDate.getOrDefault(dayInAbsence, List.of());
                double dayCapacity = 1.0;

                for (Absence otherAbsence : dayAbsences) {
                    // Don't count the current overtime absence or other overtime absences
                    if (otherAbsence.absenceTypeCategory() != OVERTIME) {
                        dayCapacity -= otherAbsence.dayLength().getValue();
                    }
                }

                // Ensure capacity doesn't go negative
                effectiveWorkingDays += Math.max(0, dayCapacity);
            }
        }
        return effectiveWorkingDays;
    }

    /**
     * Calculates the dayLength value that should be worked at a day. OvertimeReduction is ignored in this calculation.
     *
     * @param absences absences at a certain day
     * @return the dayLength value that should be worked at a day (1, 0.5 or 0)
     */
    private static double getShouldWorkDayLengthCapacity(List<Absence> absences) {
        double capacity = 1.0;
        for (Absence otherAbsence : absences) {
            // ignore overtime reduction since capacity is required to calculate distributed overtime hours.
            // if it would not be ignored, the capacity would be 0 and the distribution could not be calculated anymore.
            if (otherAbsence.absenceTypeCategory() != OVERTIME) {
                capacity -= otherAbsence.dayLength().getValue();
            }
        }
        capacity = Math.max(0, capacity);
        return capacity;
    }
}
