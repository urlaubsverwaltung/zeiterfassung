package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * @param locked              whether this day is locked or not.
 *                            Note that this does not include whether it can be bypassed or not by a privileged person!
 * @param date                of the time entry day
 * @param plannedWorkingHours planned working hours
 * @param shouldWorkingHours  should working hours
 * @param timeEntries         list of time entries
 * @param absences            list of absences. could be one FULL absence or two absences MORNING and NOON
 */
record TimeEntryDay(
    boolean locked,
    LocalDate date,
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

    public WorkDuration workDuration() {
        return timeEntries
            .stream()
            .map(TimeEntry::workDuration)
            .reduce(WorkDuration.ZERO, WorkDuration::plus);
    }
}
