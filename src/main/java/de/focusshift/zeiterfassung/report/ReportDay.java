package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;

import java.time.Duration;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

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

        final Map<UserIdComposite, Double> absenceLengthByUser = detailDayAbsencesByUser.entrySet().stream()
            .map(entry ->
                new AbstractMap.SimpleEntry<>(
                    entry.getKey(),
                    entry.getValue().stream().map(ReportDayAbsence::absence)
                        .map(Absence::dayLength)
                        .map(DayLength::getValue)
                        .reduce(0.0, Double::sum)
                )
            )
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        Duration plannedWorkingHoursOfAllUsers = plannedWorkingHours().duration();
        for (final Map.Entry<UserIdComposite, Double> absenceLengths : absenceLengthByUser.entrySet()) {

            final Duration plannedWorkingHoursOfUser = plannedWorkingHoursByUser.get(absenceLengths.getKey()).duration();
            final Double absenceLength = absenceLengths.getValue();

            if (absenceLength == 1.0) {
                plannedWorkingHoursOfAllUsers = plannedWorkingHoursOfAllUsers.minus(plannedWorkingHoursOfUser);
            }
            else if (absenceLength == 0.5) {
                plannedWorkingHoursOfAllUsers = plannedWorkingHoursOfAllUsers.minus(plannedWorkingHoursOfUser.dividedBy(2));
            }
        }

        return new ShouldWorkingHours(plannedWorkingHoursOfAllUsers);
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
