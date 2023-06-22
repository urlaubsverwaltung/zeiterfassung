package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.overtime.OvertimeDuration;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.lang.invoke.MethodHandles.lookup;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.util.function.Function.identity;
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
        final List<User> users = List.of(user);

        return createReportWeek(year, week, users,
            period -> Map.of(userLocalId, timeEntryService.getEntries(period.from(), period.toExclusive(), userId)),
            period -> workingTimeCalendarService.getWorkingTimes(period.from(), period.toExclusive(), List.of(userLocalId)));
    }

    ReportWeek getReportWeek(Year year, int week, List<UserLocalId> userLocalIds) {
        final List<User> users = userManagementService.findAllUsersByLocalIds(userLocalIds);
        return createReportWeek(year, week, users,
            period -> timeEntryService.getEntriesByUsers(period.from(), period.toExclusive(), users),
            period -> workingTimeCalendarService.getWorkingTimes(period.from(), period.toExclusive(), userLocalIds));
    }

    ReportWeek getReportWeekForAllUsers(Year year, int week) {
        final List<User> users = userManagementService.findAllUsers();
        final List<UserLocalId> userLocalIds = users.stream().map(User::localId).toList();
        return createReportWeek(year, week, users,
            period -> timeEntryService.getEntriesByUsers(period.from(), period.toExclusive(), users),
            period -> workingTimeCalendarService.getWorkingTimes(period.from(), period.toExclusive(), userLocalIds));
    }

    ReportMonth getReportMonth(YearMonth yearMonth, UserId userId) {

        final User user = userManagementService.findUserById(userId)
            .orElseThrow(() -> new IllegalStateException("could not find user id=%s".formatted(userId)));

        final List<User> users = List.of(user);

        return createReportMonth(yearMonth, users,
            period -> timeEntryService.getEntriesByUsers(period.from(), period.toExclusive(), users),
            period -> workingTimeCalendarService.getWorkingTimes(period.from(), period.toExclusive(), List.of(user.localId())));
    }

    ReportMonth getReportMonth(YearMonth yearMonth, List<UserLocalId> userLocalIds) {
        final List<User> users = userManagementService.findAllUsersByLocalIds(userLocalIds);
        return createReportMonth(yearMonth, users,
            period -> timeEntryService.getEntriesByUsers(period.from(), period.toExclusive(), users),
            period -> workingTimeCalendarService.getWorkingTimes(period.from(), period.toExclusive(), userLocalIds));
    }

    ReportMonth getReportMonthForAllUsers(YearMonth yearMonth) {
        final List<User> users = userManagementService.findAllUsers();
        final List<UserLocalId> userLocalIds = users.stream().map(User::localId).toList();
        return createReportMonth(yearMonth, users,
            period -> timeEntryService.getEntriesByUsers(period.from(), period.toExclusive(), users),
            period -> workingTimeCalendarService.getWorkingTimes(period.from(), period.toExclusive(), userLocalIds));
    }

    private ReportWeek createReportWeek(Year year, int week, List<User> users,
                                        Function<Period, Map<UserLocalId, List<TimeEntry>>> timeEntriesProvider,
                                        Function<Period, Map<UserLocalId, WorkingTimeCalendar>> workingTimeCalendarProvider) {

        final LocalDate firstDateOfWeek = userDateService.firstDayOfWeek(year, week);
        final Period period = new Period(firstDateOfWeek, firstDateOfWeek.plusWeeks(1));

        final Map<UserId, User> userById = users.stream().collect(toMap(User::id, identity()));
        final Map<UserLocalId, User> userByLocalId = users.stream().collect(toMap(User::localId, identity()));

        final Map<UserLocalId, List<TimeEntry>> timeEntries = timeEntriesProvider.apply(period);

        final Map<UserLocalId, WorkingTimeCalendar> workingTimeCalendars = workingTimeCalendarProvider.apply(period);
        final Function<LocalDate, Map<User, PlannedWorkingHours>> plannedWorkingTimeByDate = plannedWorkingTimeForDate(workingTimeCalendars, userByLocalId);

        // overtime is shown only for the visible week. not the accumulated overtime until this firstDateOfWeek.
        // therefore create a map of OvertimeDuration.ZERO
        final Map<UserLocalId, OvertimeDuration> startOfWeekOvertimeByUser = users.stream().collect(toMap(User::localId, (unused) -> OvertimeDuration.ZERO));

        return reportWeek(firstDateOfWeek, timeEntries, userById, plannedWorkingTimeByDate, startOfWeekOvertimeByUser);
    }

    private ReportMonth createReportMonth(YearMonth yearMonth, List<User> users,
                                          Function<Period, Map<UserLocalId, List<TimeEntry>>> timeEntriesProvider,
                                          Function<Period, Map<UserLocalId, WorkingTimeCalendar>> workingTimeCalendarProvider) {

        final LocalDate firstOfMonth = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), 1);
        final Period period = new Period(firstOfMonth, firstOfMonth.plusMonths(1));

        final Map<UserId, User> userById = users.stream().collect(toMap(User::id, identity()));
        final Map<UserLocalId, User> userByLocalId = users.stream().collect(toMap(User::localId, identity()));

        final Map<UserLocalId, List<TimeEntry>> timeEntriesByUserId = timeEntriesProvider.apply(period);
        final Map<UserLocalId, WorkingTimeCalendar> workingTimeCalendars = workingTimeCalendarProvider.apply(period);
        final Function<LocalDate, Map<User, PlannedWorkingHours>> plannedWorkingTimeForDate = plannedWorkingTimeForDate(workingTimeCalendars, userByLocalId);
        final Map<UserLocalId, OvertimeDuration> overtimeStartOfWeekByUser = users.stream().collect(toMap(User::localId, (unused) -> OvertimeDuration.ZERO));

        final List<ReportWeek> weeks = new ArrayList<>();

        for (LocalDate startOfWeek : getStartOfWeekDatesForMonth(yearMonth)) {
            final ReportWeek reportWeek = reportWeek(
                startOfWeek,
                timeEntriesByUserId,
                userById,
                plannedWorkingTimeForDate,
                overtimeStartOfWeekByUser
            );
            // add overtime to `overtimeStartOfWeekByUser` for next week
            overtimeStartOfWeekByUser.replaceAll(plusOvertimeDuration(reportWeek.overtimeDurationEndOfWeekByUser()));
            weeks.add(reportWeek);
        }

        return new ReportMonth(yearMonth, weeks);
    }

    private static BiFunction<UserLocalId, OvertimeDuration, OvertimeDuration> plusOvertimeDuration(Map<UserLocalId, OvertimeDuration> overtimeDurationEndOfWeekByUser) {
        return (userLocalId, overtimeDuration) -> overtimeDurationEndOfWeekByUser.get(userLocalId).plus(overtimeDuration);
    }

    private Function<LocalDate, Map<User, PlannedWorkingHours>> plannedWorkingTimeForDate(
        Map<UserLocalId, WorkingTimeCalendar> workingTimeCalendars, Map<UserLocalId, User> userByLocalId) {

        return date ->
            workingTimeCalendars.entrySet()
                .stream()
                .collect(
                    toMap(
                        key -> userByLocalId.get(key.getKey()),
                        entry -> entry.getValue().plannedWorkingHours(date).orElse(PlannedWorkingHours.ZERO)
                    )
                );
    }

    private ReportWeek reportWeek(LocalDate startOfWeekDate,
                                  Map<UserLocalId, List<TimeEntry>> timeEntriesByUserLocalId,
                                  Map<UserId, User> userById,
                                  Function<LocalDate, Map<User, PlannedWorkingHours>> plannedWorkingHoursProvider,
                                  Map<UserLocalId, OvertimeDuration> startOfWeekOvertimeByUser) {

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

        // initial overtime. will be updated in the day iteration below.
        final Map<UserLocalId, OvertimeDuration> overtimeStartOfDayByUser = new HashMap<>(startOfWeekOvertimeByUser);

        final List<ReportDay> reportDays = IntStream.rangeClosed(0, 6)
            .mapToObj(daysToAdd -> {
                final LocalDate date = startOfWeekDate.plusDays(daysToAdd);
                final Map<User, PlannedWorkingHours> plannedWorkingHoursByUser = plannedWorkingHoursProvider.apply(date);
                final Map<UserLocalId, List<ReportDayEntry>> dayEntriesByUser = resolveReportDayEntries.apply(date);
                final ReportDay day = new ReportDay(date, plannedWorkingHoursByUser, new HashMap<>(overtimeStartOfDayByUser), dayEntriesByUser);

                final Map<UserLocalId, OvertimeDuration> nextOvertimeStartOfDayByUser = day.accumulatedOvertimeToDateEndOfBusinessByUser();

                // summarize overtime for existing persons.
                overtimeStartOfDayByUser.replaceAll(nextOvertimeStartOfDayByUser::getOrDefault);
                // add overtime for new persons
                nextOvertimeStartOfDayByUser.forEach(overtimeStartOfDayByUser::putIfAbsent);

                return day;
            })
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

    private static boolean isPreviousMonth(LocalDate possiblePreviousMonthDate, YearMonth yearMonth) {
        return YearMonth.from(possiblePreviousMonthDate).until(yearMonth, MONTHS) == 1;
    }
}
