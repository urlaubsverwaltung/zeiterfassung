package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

/**
 * Report information for a certain date and users.
 *
 * <p>
 * All byUser Maps contains values for the same keys. (Please ensure this on constructing this object.)
 *
 * @param date
 * @param locked                    true if date is locked for adding/editing time entries
 * @param workingTimeCalendarByUser {@linkplain WorkingTimeCalendar} for all relevant users
 * @param reportDayEntriesByUser    {@linkplain ReportDayEntry entries} for all relevant users
 * @param workDurationByUser        calculated {@linkplain WorkDuration} for all relevant users
 * @param detailDayAbsencesByUser   {@linkplain ReportDayAbsence absences} for all relevant users
 */
public record ReportDay(
    LocalDate date,
    boolean locked,
    Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendarByUser,
    Map<UserIdComposite, List<ReportDayEntry>> reportDayEntriesByUser,
    Map<UserIdComposite, WorkDuration> workDurationByUser,
    Map<UserIdComposite, List<ReportDayAbsence>> detailDayAbsencesByUser
) implements HasWorkDurationByUser {

    public List<ReportDayEntry> reportDayEntries() {
        return reportDayEntriesByUser.values().stream().flatMap(Collection::stream).toList();
    }

    public PlannedWorkingHours plannedWorkingHours() {
        return workingTimeCalendarByUser.values().stream()
            .map(calendar -> calendar.plannedWorkingHours(date))
            .flatMap(Optional::stream)
            .reduce(PlannedWorkingHours.ZERO, PlannedWorkingHours::plus);
    }

    public Map<UserIdComposite, PlannedWorkingHours> plannedWorkingHoursByUser() {
        return workingTimeCalendarByUser.entrySet().stream().collect(toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().plannedWorkingHours(date).orElse(PlannedWorkingHours.ZERO)
        ));
    }

    public ShouldWorkingHours shouldWorkingHours() {
        return workingTimeCalendarByUser.values().stream()
            .map(calendar -> calendar.shouldWorkingHours(date))
            .flatMap(Optional::stream)
            .reduce(ShouldWorkingHours.ZERO, ShouldWorkingHours::plus);
    }

    public Map<UserIdComposite, ShouldWorkingHours> shouldWorkingHoursByUser() {
        return workingTimeCalendarByUser.entrySet().stream()
            .collect(toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().shouldWorkingHours(date).orElse(ShouldWorkingHours.ZERO))
            );
    }

    public WorkDuration workDuration() {
        return workDurationByUser.values().stream().reduce(WorkDuration.ZERO, WorkDuration::plus);
    }
}
