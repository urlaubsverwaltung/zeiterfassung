package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

record ReportDay(
    LocalDate date,
    Map<UserIdComposite, PlannedWorkingHours> plannedWorkingHoursByUser,
    Map<UserIdComposite, List<ReportDayEntry>> reportDayEntriesByUser,
    Map<UserIdComposite, List<ReportDayAbsence>> detailDayAbsencesByUser
) {

    public List<ReportDayEntry> reportDayEntries() {
        return reportDayEntriesByUser.values().stream().flatMap(Collection::stream).toList();
    }

    public PlannedWorkingHours plannedWorkingHours() {
        return plannedWorkingHoursByUser.values().stream().reduce(PlannedWorkingHours.ZERO, PlannedWorkingHours::plus);
    }

    public ShouldWorkingHours shouldWorkingHours() {

        final double absenceDayLengthValue = detailDayAbsencesByUser.values().stream()
            .flatMap(Collection::stream)
            .map(ReportDayAbsence::absence)
            .map(Absence::dayLength)
            .map(DayLength::getValue)
            .reduce(0.0, Double::sum);

        if (absenceDayLengthValue >= 1.0) {
            return ShouldWorkingHours.ZERO;
        }

        final PlannedWorkingHours plannedWorkingHours = plannedWorkingHours();

        if (absenceDayLengthValue == 0.5) {
            return new ShouldWorkingHours(plannedWorkingHours.duration().dividedBy(2));
        }

        return new ShouldWorkingHours(plannedWorkingHours.duration());
    }

    public PlannedWorkingHours plannedWorkingHoursByUser(UserLocalId userLocalId) {
        return findValueByFirstKeyMatch(plannedWorkingHoursByUser, userIdComposite -> userLocalId.equals(userIdComposite.localId()))
            .orElse(PlannedWorkingHours.ZERO);
    }

    public WorkDuration workDuration() {

        final Stream<ReportDayEntry> allReportDayEntries = reportDayEntriesByUser.values()
            .stream()
            .flatMap(Collection::stream);

        return calculateWorkDurationFrom(allReportDayEntries);
    }

    public WorkDuration workDurationByUser(UserLocalId userLocalId) {
        return workDurationByUserPredicate(userIdComposite -> userLocalId.equals(userIdComposite.localId()));
    }

    private WorkDuration workDurationByUserPredicate(Predicate<UserIdComposite> predicate) {
        final List<ReportDayEntry> reportDayEntries = findValueByFirstKeyMatch(reportDayEntriesByUser, predicate).orElse(List.of());
        return calculateWorkDurationFrom(reportDayEntries.stream());
    }

    private WorkDuration calculateWorkDurationFrom(Stream<ReportDayEntry> reportDayEntries) {
        return reportDayEntries
            .map(ReportDayEntry::workDuration)
            .reduce(WorkDuration.ZERO, WorkDuration::plus);
    }

    private <K, T> Optional<T> findValueByFirstKeyMatch(Map<K, T> map, Predicate<K> predicate) {
        return map.entrySet()
            .stream()
            .filter(entry -> predicate.test(entry.getKey()))
            .findFirst()
            .map(Map.Entry::getValue);
    }
}
