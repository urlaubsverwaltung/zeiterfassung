package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.usermanagement.WorkingTimeCalendar;
import de.focusshift.zeiterfassung.usermanagement.WorkingTimeCalendarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.lang.invoke.MethodHandles.lookup;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
class ReportServiceRaw {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    record Period(LocalDate from, LocalDate toExclusive) {
    }

    private final TimeEntryService timeEntryService;
    private final UserManagementService userManagementService;
    private final UserDateService userDateService;
    private final WorkingTimeCalendarService workingTimeCalendarService;

    ReportServiceRaw(TimeEntryService timeEntryService, UserManagementService userManagementService,
                     UserDateService userDateService, WorkingTimeCalendarService workingTimeCalendarService) {

        this.timeEntryService = timeEntryService;
        this.userManagementService = userManagementService;
        this.userDateService = userDateService;
        this.workingTimeCalendarService = workingTimeCalendarService;
    }

    ReportWeek getReportWeek(Year year, int week, UserId userId) {

        final User user = userManagementService.findUserById(userId)
            .orElseThrow(() -> new IllegalStateException("could not find user id=%s".formatted(userId)));

        final UserLocalId userLocalId = user.localId();

        return createReportWeek(year, week,
            period -> Map.of(userLocalId, timeEntryService.getEntries(period.from(), period.toExclusive(), userId)),
            period -> workingTimeCalendarService.getWorkingTimes(period.from(), period.toExclusive(), List.of(userLocalId)));
    }

    ReportWeek getReportWeek(Year year, int week, List<UserLocalId> userLocalIds) {
        return createReportWeek(year, week,
            period -> timeEntryService.getEntriesByUserLocalIds(period.from(), period.toExclusive(), userLocalIds),
            period -> workingTimeCalendarService.getWorkingTimes(period.from(), period.toExclusive(), userLocalIds));
    }

    ReportWeek getReportWeekForAllUsers(Year year, int week) {
        return createReportWeek(year, week,
            period -> timeEntryService.getEntriesForAllUsers(period.from(), period.toExclusive()),
            period -> workingTimeCalendarService.getWorkingTimes(period.from(), period.toExclusive()));
    }

    ReportMonth getReportMonth(YearMonth yearMonth, UserId userId) {

        final User user = userManagementService.findUserById(userId)
            .orElseThrow(() -> new IllegalStateException("could not find user id=%s".formatted(userId)));

        final UserLocalId userLocalId = user.localId();

        return createReportMonth(yearMonth,
            period -> timeEntryService.getEntriesByUserLocalIds(period.from(), period.toExclusive(), List.of(userLocalId)),
            period -> workingTimeCalendarService.getWorkingTimes(period.from(), period.toExclusive(), List.of(user.localId())));
    }

    ReportMonth getReportMonth(YearMonth yearMonth, List<UserLocalId> userLocalIds) {
        return createReportMonth(yearMonth,
            period -> timeEntryService.getEntriesByUserLocalIds(period.from(), period.toExclusive(), userLocalIds),
            period -> workingTimeCalendarService.getWorkingTimes(period.from(), period.toExclusive(), userLocalIds));
    }

    ReportMonth getReportMonthForAllUsers(YearMonth yearMonth) {
        return createReportMonth(yearMonth,
            period -> timeEntryService.getEntriesForAllUsers(period.from(), period.toExclusive()),
            period -> workingTimeCalendarService.getWorkingTimes(period.from(), period.toExclusive()));
    }

    private ReportWeek createReportWeek(Year year, int week,
                                        Function<Period, Map<UserLocalId, List<TimeEntry>>> timeEntriesProvider,
                                        Function<Period, Map<UserLocalId, WorkingTimeCalendar>> workingTimeCalendarProvider) {

        final LocalDate firstDateOfWeek = userDateService.firstDayOfWeek(year, week);

        final Period period = new Period(firstDateOfWeek, firstDateOfWeek.plusWeeks(1));

        final Map<UserLocalId, List<TimeEntry>> timeEntries = timeEntriesProvider.apply(period);
        final Map<UserId, User> userById = userByIdForTimeEntries(timeEntries.values().stream().flatMap(Collection::stream).toList());

        final Map<UserLocalId, WorkingTimeCalendar> workingTimeCalendars = workingTimeCalendarProvider.apply(period);
        final Function<LocalDate, Map<UserLocalId, PlannedWorkingHours>> plannedWorkingTimeByDate = plannedWorkingTimeForDate(workingTimeCalendars);

        return reportWeek(firstDateOfWeek, timeEntries, userById, plannedWorkingTimeByDate);
    }

    private ReportMonth createReportMonth(YearMonth yearMonth,
                                          Function<Period, Map<UserLocalId, List<TimeEntry>>> timeEntriesProvider,
                                          Function<Period, Map<UserLocalId, WorkingTimeCalendar>> workingTimeCalendarProvider) {

        final LocalDate firstOfMonth = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), 1);

        final Period period = new Period(firstOfMonth, firstOfMonth.plusMonths(1));

        final Map<UserLocalId, List<TimeEntry>> timeEntriesByUserId = timeEntriesProvider.apply(period);
        final Map<UserId, User> userById = userByIdForTimeEntries(timeEntriesByUserId.values().stream().flatMap(Collection::stream).toList());

        final Map<UserLocalId, WorkingTimeCalendar> workingTimeCalendars = workingTimeCalendarProvider.apply(period);
        final Function<LocalDate, Map<UserLocalId, PlannedWorkingHours>> plannedWorkingTimeForDate = plannedWorkingTimeForDate(workingTimeCalendars);

        final List<ReportWeek> weeks = getStartOfWeekDatesForMonth(yearMonth)
            .stream()
            .map(startOfWeekDate ->
                reportWeek(
                    startOfWeekDate,
                    timeEntriesByUserId,
                    userById,
                    plannedWorkingTimeForDate
                )
            )
            .toList();

        return new ReportMonth(yearMonth, weeks);
    }

    private Function<LocalDate, Map<UserLocalId, PlannedWorkingHours>> plannedWorkingTimeForDate(Map<UserLocalId, WorkingTimeCalendar> workingTimeCalendars) {
        return date ->
            workingTimeCalendars.entrySet()
                .stream()
                .collect(
                    toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().plannedWorkingHours(date).orElse(PlannedWorkingHours.ZERO)
                    )
                );
    }

    private Map<UserId, User> userByIdForTimeEntries(List<TimeEntry> timeEntries) {
        final List<UserId> userIds = timeEntries.stream().map(TimeEntry::userId).distinct().toList();
        return userManagementService.findAllUsersByIds(userIds)
            .stream()
            .collect(toMap(User::id, Function.identity()));
    }

    private ReportWeek reportWeek(LocalDate startOfWeekDate,
                                  Map<UserLocalId, List<TimeEntry>> timeEntriesByUserLocalId,
                                  Map<UserId, User> userById,
                                  Function<LocalDate, Map<UserLocalId, PlannedWorkingHours>> plannedWorkingHoursProvider) {

        final Map<LocalDate, Map<UserLocalId, List<ReportDayEntry>>> reportEntriesByDate = new HashMap<>();
        for (Map.Entry<UserLocalId, List<TimeEntry>> entry : timeEntriesByUserLocalId.entrySet()) {

            final UserLocalId userLocalId = entry.getKey();

            final Map<LocalDate, List<ReportDayEntry>> collect = entry.getValue()
                .stream()
                .map(t -> timeEntryToReportDayEntry(t, userById::get))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(groupingBy(report -> report.start().toLocalDate()));

            for (Map.Entry<LocalDate, List<ReportDayEntry>> localDateListEntry : collect.entrySet()) {
                reportEntriesByDate.compute(localDateListEntry.getKey(), (localDate, userLocalIdListMap) -> {
                    final Map<UserLocalId, List<ReportDayEntry>> map = userLocalIdListMap == null ? new HashMap<>() : userLocalIdListMap;
                    map.put(userLocalId, collect.get(localDate));
                    return map;
                });
            }
        }

        final Function<LocalDate, Map<UserLocalId, List<ReportDayEntry>>> resolveReportDayEntries =
            (LocalDate date) -> reportEntriesByDate.getOrDefault(date, Map.of());

        final List<ReportDay> reportDays = IntStream.rangeClosed(0, 6)
            .mapToObj(daysToAdd ->
                toReportDay(
                    startOfWeekDate.plusDays(daysToAdd),
                    plannedWorkingHoursProvider,
                    resolveReportDayEntries
                ))
            .toList();

        return new ReportWeek(startOfWeekDate, reportDays);
    }

    private List<LocalDate> getStartOfWeekDatesForMonth(YearMonth yearMonth) {
        final List<LocalDate> startOfWeekDates = new ArrayList<>();

        final LocalDate firstOfMonth = yearMonth.atDay(1);
        LocalDate date = userDateService.localDateToFirstDateOfWeek(firstOfMonth);

        while (isPreviousMonth(date, yearMonth) || date.getMonthValue() == yearMonth.getMonthValue()) {
            startOfWeekDates.add(date);
            date = date.plusWeeks(1);
        }

        return startOfWeekDates;
    }

    private static Optional<ReportDayEntry> timeEntryToReportDayEntry(TimeEntry timeEntry, Function<UserId, User> userProvider) {

        final String comment = timeEntry.comment();
        final ZonedDateTime startDateTime = timeEntry.start();
        final ZonedDateTime endDateTime = timeEntry.end();

        final User user = userProvider.apply(timeEntry.userId());
        if (user == null) {
            LOG.info("could not find user with id={} for timeEntry={} while generating report.", timeEntry.userId(), timeEntry.id());
            return Optional.empty();
        }

        final ReportDayEntry first = new ReportDayEntry(user, comment, startDateTime, endDateTime, timeEntry.isBreak());
        return Optional.of(first);
    }

    private static ReportDay toReportDay(LocalDate date,
                                         Function<LocalDate, Map<UserLocalId, PlannedWorkingHours>> plannedWorkingHoursProvider,
                                         Function<LocalDate, Map<UserLocalId, List<ReportDayEntry>>> resolveReportDayEntries) {

        return new ReportDay(date, plannedWorkingHoursProvider.apply(date), resolveReportDayEntries.apply(date));
    }

    private static boolean isPreviousMonth(LocalDate possiblePreviousMonthDate, YearMonth yearMonth) {
        return YearMonth.from(possiblePreviousMonthDate).until(yearMonth, MONTHS) == 1;
    }
}
