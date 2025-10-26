package de.focusshift.zeiterfassung.timeentry;


import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Describes a bucket of multiple {@link TimeEntry} elements for a given date.
 *
 * <p>
 * A notable difference to a single {@link TimeEntry} is the calculated {@link TimeEntryDay#workDuration()}. While a single
 * {@link TimeEntry#workDuration()} is just the duration between start and end, the {@link TimeEntryDay#workDuration()}
 * is calculated based on a user selected strategy.
 * See {@link de.focusshift.zeiterfassung.workduration.WorkDurationCalculationService} for more detail.
 *
 * @param locked              whether this day is locked or not.
 *                            Note that this does not include whether it can be bypassed or not by a privileged person!
 * @param date                of the time entry day
 * @param workDuration        the calculated {@link WorkDuration} of this day
 * @param plannedWorkingHours planned working hours
 * @param shouldWorkingHours  should working hours
 * @param timeEntries         list of time entries
 * @param absences            list of absences. could be one FULL absence or two absences MORNING and NOON
 */
public record TimeEntryDay(
    boolean locked,
    LocalDate date,
    WorkDuration workDuration,
    PlannedWorkingHours plannedWorkingHours,
    ShouldWorkingHours shouldWorkingHours,
    List<TimeEntry> timeEntries,
    List<Absence> absences
) implements HasWorkedHoursRatio {

    /**
     * @return overtime {@linkplain Duration}. can be negative.
     */
    public Duration overtime() {
        return workDuration().durationInMinutes().minus(shouldWorkingHours.durationInMinutes());
    }
}
