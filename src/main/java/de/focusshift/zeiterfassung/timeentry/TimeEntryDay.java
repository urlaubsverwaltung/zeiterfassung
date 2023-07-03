package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.DayLength;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static java.math.RoundingMode.CEILING;

record TimeEntryDay(LocalDate date, PlannedWorkingHours plannedWorkingHours, List<TimeEntry> timeEntries, List<Absence> absences) {

    public PlannedWorkingHours plannedWorkingHours() {

        final double absenceDayLengthValue = absences.stream()
            .map(Absence::dayLength)
            .map(DayLength::getValue)
            .reduce(0.0, Double::sum);

        if (absenceDayLengthValue >= 1.0) {
            return PlannedWorkingHours.ZERO;
        } else if (absenceDayLengthValue == 0.5) {
            return new PlannedWorkingHours(plannedWorkingHours.value().dividedBy(2));
        }

        return plannedWorkingHours;
    }

    /**
     *
     * @return overtime {@linkplain Duration}. can be negative.
     */
    public Duration overtime() {
        return workDuration().minutes().minus(plannedWorkingHours.minutes());
    }

    public WorkDuration workDuration() {

        final Duration duration = timeEntries
            .stream()
            .map(TimeEntry::workDuration)
            .map(WorkDuration::value)
            .reduce(Duration.ZERO, Duration::plus);

        return new WorkDuration(duration);
    }

    /**
     * Ratio of worked hours to planned hours. Does not include absences like public holidays.
     *
     * @return value between 0 and 1
     */
    public BigDecimal workedHoursRatio() {

        final double planned = plannedWorkingHours.hoursDoubleValue();
        final double worked = workDuration().hoursDoubleValue();

        if (worked == 0) {
            return BigDecimal.ZERO;
        }

        if (planned == 0) {
            return BigDecimal.ONE;
        }

        final BigDecimal ratio = BigDecimal.valueOf(worked).divide(BigDecimal.valueOf(planned), 2, CEILING);

        return ratio.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : ratio;
    }
}
