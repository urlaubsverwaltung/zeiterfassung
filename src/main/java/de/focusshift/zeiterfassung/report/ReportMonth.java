package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.HasWorkedHoursRatio;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static de.focusshift.zeiterfassung.report.ReportFunctions.calculateAverageDayWorkDuration;
import static de.focusshift.zeiterfassung.report.ReportFunctions.summarizePlannedWorkingHours;
import static de.focusshift.zeiterfassung.report.ReportFunctions.summarizePlannedWorkingHoursByUser;
import static de.focusshift.zeiterfassung.report.ReportFunctions.summarizeShouldWorkingHours;
import static de.focusshift.zeiterfassung.report.ReportFunctions.summarizeShouldWorkingHoursByUser;
import static de.focusshift.zeiterfassung.report.ReportFunctions.summarizeWorkDuration;
import static de.focusshift.zeiterfassung.report.ReportFunctions.summarizeWorkDurationByUser;

record ReportMonth(YearMonth yearMonth, List<ReportWeek> weeks) implements HasWorkDurationByUser, HasWorkedHoursRatio {

    public PlannedWorkingHours plannedWorkingHours() {
        return summarizePlannedWorkingHours(weeks, ReportWeek::plannedWorkingHours);
    }

    public Map<UserIdComposite, PlannedWorkingHours> plannedWorkingHoursByUser() {
        return summarizePlannedWorkingHoursByUser(weeks);
    }

    public ShouldWorkingHours shouldWorkingHours() {
        return summarizeShouldWorkingHours(weeks, ReportWeek::shouldWorkingHours);
    }

    public Map<UserIdComposite, ShouldWorkingHours> shouldWorkingHoursByUser() {
        return summarizeShouldWorkingHoursByUser(weeks);
    }

    public WorkDuration averageDayWorkDuration() {
        return calculateAverageDayWorkDuration(weeks, ReportWeek::averageDayWorkDuration);
    }

    @Override
    public WorkDuration workDuration() {
        return summarizeWorkDuration(weeks, ReportWeek::workDuration);
    }

    public Map<UserIdComposite, WorkDuration> workDurationByUser() {
        return summarizeWorkDurationByUser(weeks);
    }
}
