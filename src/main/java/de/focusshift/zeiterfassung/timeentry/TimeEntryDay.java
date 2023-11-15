package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.absence.Absence;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static java.math.RoundingMode.CEILING;

/**
 *
 * @param date of the time entry day
 * @param plannedWorkingHours planned working hours
 * @param shouldWorkingHours should working hours
 * @param timeEntries list of time entries
 * @param absences list of absences. could be one FULL absence or two absences MORNING and NOON
 */
record TimeEntryDay(
    LocalDate date,
    PlannedWorkingHours plannedWorkingHours,
    ShouldWorkingHours shouldWorkingHours,
    List<TimeEntry> timeEntries,
    List<Absence> absences
) {

    /**
     *
     * @return overtime {@linkplain Duration}. can be negative.
     */
    public Duration overtime() {
        return workDuration().durationInMinutes().minus(shouldWorkingHours.durationInMinutes());
    }

    public WorkDuration workDuration() {

        final Duration duration = timeEntries
            .stream()
            .map(TimeEntry::workDuration)
            .map(WorkDuration::duration)
            .reduce(Duration.ZERO, Duration::plus);

        return new WorkDuration(duration);
    }

    /**
     * Ratio of worked hours to planned hours. Does not include absences like public holidays.
     *
     * @return value between 0 and 1
     */
    public BigDecimal workedHoursRatio() {

        final double should = shouldWorkingHours.hoursDoubleValue();
        final double worked = workDuration().hoursDoubleValue();

        if (worked == 0) {
            return BigDecimal.ZERO;
        }

        if (should == 0) {
            return BigDecimal.ONE;
        }

        final BigDecimal ratio = BigDecimal.valueOf(worked).divide(BigDecimal.valueOf(should), 2, CEILING);

        return ratio.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : ratio;
    }
}
