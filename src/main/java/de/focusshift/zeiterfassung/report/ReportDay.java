package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.overtime.OvertimeDuration;
import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;

record ReportDay(
    LocalDate date,
    Map<User, PlannedWorkingHours> plannedWorkingHoursByUser,
    Map<UserLocalId, OvertimeDuration> accumulatedOvertimeToDateByUser,
    Map<UserLocalId, List<ReportDayEntry>> reportDayEntriesByUser
) {

    public List<ReportDayEntry> reportDayEntries() {
        return reportDayEntriesByUser.values().stream().flatMap(Collection::stream).toList();
    }

    public PlannedWorkingHours plannedWorkingHours() {
        return plannedWorkingHoursByUser.values().stream().reduce(PlannedWorkingHours.ZERO, PlannedWorkingHours::plus);
    }

    public Optional<OvertimeDuration> accumulatedOvertimeToDateByUser(UserLocalId userLocalId) {
        return findValueByFirstKeyMatch(accumulatedOvertimeToDateByUser, userLocalId::equals);
    }

    public Optional<OvertimeDuration> accumulatedOvertimeToDateEndOfBusinessByUser(UserLocalId userLocalId) {

        final Optional<PlannedWorkingHours> plannedWorkingHours = plannedWorkingHoursByUser.entrySet()
            .stream()
            .filter(entry -> entry.getKey().localId().equals(userLocalId))
            .findFirst()
            .map(Map.Entry::getValue);

        final Optional<OvertimeDuration> overtimeStartOfBusiness = accumulatedOvertimeToDateByUser(userLocalId);

        if (plannedWorkingHours.isEmpty()) {
            // TODO how to handle `plannedWorkingHours=null`? it should be `plannedWorkingHours=ZERO` when everything is ok. `null` should only the case for an unknown `userLocalId` i think.
            return overtimeStartOfBusiness;
        }

        // calculate working time duration of this day
        // to add it to `overtimeStartOfBusiness`

        final WorkDuration workDurationThisDay = reportDayEntriesByUser.getOrDefault(userLocalId, List.of())
            .stream()
            .filter(not(ReportDayEntry::isBreak))
            .map(ReportDayEntry::workDuration)
            .reduce(WorkDuration.ZERO, WorkDuration::plus);

        final Duration overtimeDurationThisDay = plannedWorkingHours.get().value().negated().plus(workDurationThisDay.value());
        final OvertimeDuration overtimeEndOfBusiness = overtimeStartOfBusiness.orElse(OvertimeDuration.ZERO).plus(new OvertimeDuration(overtimeDurationThisDay));
        return Optional.of(overtimeEndOfBusiness);
    }

    public Map<UserLocalId, OvertimeDuration> accumulatedOvertimeToDateEndOfBusinessByUser() {
        // `accumulatedOvertimeToDateByUser` could not contain persons with timeEntries at this day.
        // we need to iterate ALL persons that should have worked this day.
        final Map<UserLocalId, OvertimeDuration> collect = plannedWorkingHoursByUser.keySet()
            .stream()
            .map(user -> Map.entry(user.localId(), accumulatedOvertimeToDateEndOfBusinessByUser(user.localId()).orElse(OvertimeDuration.ZERO)))
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        return collect;
    }

    public WorkDuration workDuration() {

        final Stream<ReportDayEntry> allReportDayEntries = reportDayEntriesByUser.values()
            .stream()
            .flatMap(Collection::stream);

        return calculateWorkDurationFrom(allReportDayEntries);
    }

    private WorkDuration calculateWorkDurationFrom(Stream<ReportDayEntry> reportDayEntries) {

        final Duration duration = reportDayEntries
            .map(ReportDayEntry::workDuration)
            .map(WorkDuration::value)
            .reduce(Duration.ZERO, Duration::plus);

        return new WorkDuration(duration);
    }

    private <K, T> Optional<T> findValueByFirstKeyMatch(Map<K, T> map, Predicate<K> predicate) {
        return findValueByFirstKeyMatch(map, predicate, identity());
    }

    private <K, T, M> Optional<T> findValueByFirstKeyMatch(Map<K, T> map, Predicate<M> predicate, Function<K, M> keyMapper) {
        return map.entrySet()
            .stream()
            .filter(entry -> predicate.test(keyMapper.apply(entry.getKey())))
            .findFirst()
            .map(Map.Entry::getValue);
    }
}
