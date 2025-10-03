package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.HasWorkedHoursRatio;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Map;

import static de.focusshift.zeiterfassung.report.ReportFunctions.calculateAverageDayWorkDuration;
import static de.focusshift.zeiterfassung.report.ReportFunctions.summarizePlannedWorkingHours;
import static de.focusshift.zeiterfassung.report.ReportFunctions.summarizePlannedWorkingHoursByUser;
import static de.focusshift.zeiterfassung.report.ReportFunctions.summarizeShouldWorkingHours;
import static de.focusshift.zeiterfassung.report.ReportFunctions.summarizeShouldWorkingHoursByUser;
import static de.focusshift.zeiterfassung.report.ReportFunctions.summarizeWorkDuration;
import static de.focusshift.zeiterfassung.report.ReportFunctions.summarizeWorkDurationByUser;
import static java.util.Locale.GERMANY;

record ReportWeek(LocalDate firstDateOfWeek, List<ReportDay> reportDays) implements HasWorkDurationByUser, HasWorkedHoursRatio {

    public PlannedWorkingHours plannedWorkingHours() {
        return summarizePlannedWorkingHours(reportDays, ReportDay::plannedWorkingHours);
    }

    public Map<UserIdComposite, PlannedWorkingHours> plannedWorkingHoursByUser() {
        return summarizePlannedWorkingHoursByUser(reportDays);
    }

    public ShouldWorkingHours shouldWorkingHours() {
        return summarizeShouldWorkingHours(reportDays, ReportDay::shouldWorkingHours);
    }

    public Map<UserIdComposite, ShouldWorkingHours> shouldWorkingHoursByUser() {
        return summarizeShouldWorkingHoursByUser(reportDays);
    }

    public WorkDuration averageDayWorkDuration() {
        return calculateAverageDayWorkDuration(reportDays, ReportDay::workDuration);
    }

    @Override
    public WorkDuration workDuration() {
        return summarizeWorkDuration(this.reportDays, ReportDay::workDuration);
    }

    public Map<UserIdComposite, WorkDuration> workDurationByUser() {
        return summarizeWorkDurationByUser(this.reportDays);
    }

    public LocalDate lastDateOfWeek() {
        return firstDateOfWeek.plusDays(6);
    }

    public int calenderWeek() {
        final int minimalDaysInFirstWeek = WeekFields.of(GERMANY).getMinimalDaysInFirstWeek();
        final WeekFields weekFields = WeekFields.of(firstDateOfWeek.getDayOfWeek(), minimalDaysInFirstWeek);
        return firstDateOfWeek.get(weekFields.weekOfWeekBasedYear());
    }
}
