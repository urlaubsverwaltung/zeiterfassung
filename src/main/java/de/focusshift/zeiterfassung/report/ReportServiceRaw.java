package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendarService;
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
import java.util.stream.Stream;

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

        return createReportWeek(year, week,
            period -> Map.of(user.userIdComposite(), timeEntryService.getEntries(period.from(), period.toExclusive(), userId)),
            period -> workingTimeCalendarService.getWorkingTimeCalendarForUsers(period.from(), period.toExclusive(), List.of(user.userLocalId())));
    }

    ReportWeek getReportWeek(Year year, int week, List<UserLocalId> userLocalIds) {
        return createReportWeek(year, week,
            period -> timeEntryService.getEntriesByUserLocalIds(period.from(), period.toExclusive(), userLocalIds),
            period -> workingTimeCalendarService.getWorkingTimeCalendarForUsers(period.from(), period.toExclusive(), userLocalIds));
    }

    ReportWeek getReportWeekForAllUsers(Year year, int week) {
        return createReportWeek(year, week,
            period -> timeEntryService.getEntriesForAllUsers(period.from(), period.toExclusive()),
            period -> workingTimeCalendarService.getWorkingTimeCalendarForAllUsers(period.from(), period.toExclusive()));
    }

    ReportMonth getReportMonth(YearMonth yearMonth, UserId userId) {

        final User user = userManagementService.findUserById(userId)
            .orElseThrow(() -> new IllegalStateException("could not find user id=%s".formatted(userId)));

        return createReportMonth(yearMonth,
            period -> timeEntryService.getEntriesByUserLocalIds(period.from(), period.toExclusive(), List.of(user.userLocalId())),
            period -> workingTimeCalendarService.getWorkingTimeCalendarForUsers(period.from(), period.toExclusive(), List.of(user.userLocalId())));
    }

    ReportMonth getReportMonth(YearMonth yearMonth, List<UserLocalId> userLocalIds) {
        return createReportMonth(yearMonth,
            period -> timeEntryService.getEntriesByUserLocalIds(period.from(), period.toExclusive(), userLocalIds),
            period -> workingTimeCalendarService.getWorkingTimeCalendarForUsers(period.from(), period.toExclusive(), userLocalIds));
    }

    ReportMonth getReportMonthForAllUsers(YearMonth yearMonth) {
        return createReportMonth(yearMonth,
            period -> timeEntryService.getEntriesForAllUsers(period.from(), period.toExclusive()),
            period -> workingTimeCalendarService.getWorkingTimeCalendarForAllUsers(period.from(), period.toExclusive()));
    }

    private ReportWeek createReportWeek(Year year, int week,
                                        Function<Period, Map<UserIdComposite, List<TimeEntry>>> timeEntriesProvider,
                                        Function<Period, Map<UserIdComposite, WorkingTimeCalendar>> workingTimeCalendarProvider) {

        final LocalDate firstDateOfWeek = userDateService.firstDayOfWeek(year, week);
        final Period period = new Period(firstDateOfWeek, firstDateOfWeek.plusWeeks(1));

        final Map<UserIdComposite, List<TimeEntry>> timeEntries = timeEntriesProvider.apply(period);
        final Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendars = workingTimeCalendarProvider.apply(period);

        final Map<UserIdComposite, User> userById = userByIdFor(timeEntries, workingTimeCalendars);


        return reportWeek(firstDateOfWeek, timeEntries, workingTimeCalendars, userById);
    }

    private ReportMonth createReportMonth(YearMonth yearMonth,
                                          Function<Period, Map<UserIdComposite, List<TimeEntry>>> timeEntriesProvider,
                                          Function<Period, Map<UserIdComposite, WorkingTimeCalendar>> workingTimeCalendarProvider) {

        final LocalDate firstOfMonth = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), 1);

        final Period period = new Period(firstOfMonth, firstOfMonth.plusMonths(1));

        final Map<UserIdComposite, List<TimeEntry>> timeEntries = timeEntriesProvider.apply(period);
        final Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendars = workingTimeCalendarProvider.apply(period);
        final Map<UserIdComposite, User> userById = userByIdFor(timeEntries, workingTimeCalendars);

        final List<ReportWeek> weeks = getStartOfWeekDatesForMonth(yearMonth)
            .stream()
            .map(startOfWeekDate -> reportWeek(startOfWeekDate, timeEntries, workingTimeCalendars, userById))
            .toList();

        return new ReportMonth(yearMonth, weeks);
    }

    private Map<UserIdComposite, User> userByIdFor(Map<UserIdComposite, List<TimeEntry>> timeEntries, Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendarEntries) {

        final Stream<UserIdComposite> timeEntriesUserIds = timeEntries.values().stream().flatMap(Collection::stream).map(TimeEntry::userIdComposite);
        final Stream<UserIdComposite> workingTimeCalenderUserIds = workingTimeCalendarEntries.keySet().stream();

        final List<UserId> allUserIds = Stream.concat(timeEntriesUserIds, workingTimeCalenderUserIds).map(UserIdComposite::id).distinct().toList();

        return userManagementService.findAllUsersByIds(allUserIds)
            .stream().collect(toMap(User::userIdComposite, identity()));
    }

    private ReportWeek reportWeek(final LocalDate startOfWeekDate,
                                  final Map<UserIdComposite, List<TimeEntry>> timeEntriesByUserId,
                                  final Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendars,
                                  final Map<UserIdComposite, User> userById) {

        final Map<LocalDate, Map<UserIdComposite, List<ReportDayEntry>>> reportEntriesByDate = new HashMap<>();
        for (Map.Entry<UserIdComposite, List<TimeEntry>> entry : timeEntriesByUserId.entrySet()) {

            final UserIdComposite userIdComposite = entry.getKey();

            final Map<LocalDate, List<ReportDayEntry>> collect = entry.getValue()
                .stream()
                .map(t -> timeEntryToReportDayEntry(t, userById::get))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(groupingBy(report -> report.start().toLocalDate()));

            for (Map.Entry<LocalDate, List<ReportDayEntry>> localDateListEntry : collect.entrySet()) {
                reportEntriesByDate.compute(localDateListEntry.getKey(), (localDate, userLocalIdListMap) -> {
                    final Map<UserIdComposite, List<ReportDayEntry>> map = userLocalIdListMap == null ? new HashMap<>() : userLocalIdListMap;
                    map.put(userIdComposite, collect.get(localDate));
                    return map;
                });
            }
        }

        final Function<LocalDate, Map<UserIdComposite, List<ReportDayEntry>>> resolveReportDayEntries =
            (LocalDate date) -> reportEntriesByDate.getOrDefault(date, new HashMap<>());

        final List<ReportDay> reportDays = IntStream.rangeClosed(0, 6)
            .mapToObj(daysToAdd ->
                toReportDay(
                    startOfWeekDate.plusDays(daysToAdd),
                    userById,
                    workingTimeCalendars,
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

    private static Optional<ReportDayEntry> timeEntryToReportDayEntry(TimeEntry timeEntry, Function<UserIdComposite, User> userProvider) {

        final String comment = timeEntry.comment();
        final ZonedDateTime startDateTime = timeEntry.start();
        final ZonedDateTime endDateTime = timeEntry.end();

        final UserIdComposite userId = timeEntry.userIdComposite();
        final User user = userProvider.apply(userId);
        if (user == null) {
            LOG.info("could not find user with id={} for timeEntry={} while generating report.", userId, timeEntry.id());
            return Optional.empty();
        }

        final ReportDayEntry first = new ReportDayEntry(user, comment, startDateTime, endDateTime, timeEntry.isBreak());
        return Optional.of(first);
    }

    private static ReportDay toReportDay(LocalDate date,
                                         Map<UserIdComposite, User> userById,
                                         Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendars,
                                         Function<LocalDate, Map<UserIdComposite, List<ReportDayEntry>>> reportDayEntriesForDate) {

        final Map<UserIdComposite, List<ReportDayEntry>> reportDayEntriesByUser = reportDayEntriesForDate.apply(date);
        final Map<UserIdComposite, List<ReportDayAbsence>> reportDayAbsencesByUser = new HashMap<>();

        // to avoid NullPointer exceptions Maps must contain values for every user that has planned working hours at this day
        // todo: workingTimeCalendar map contains ALL users, we may want to handle only relevant users some time
        for (Map.Entry<UserIdComposite, WorkingTimeCalendar> entry : workingTimeCalendars.entrySet()) {
            final UserIdComposite userIdComposite = entry.getKey();
            final WorkingTimeCalendar workingTimeCalendar = entry.getValue();

            final List<Absence> absences = workingTimeCalendar.absence(date).orElse(List.of());
            final List<ReportDayAbsence> reportDayAbsences = absences.stream().map(absence -> new ReportDayAbsence(userById.get(entry.getKey()), absence)).toList();

            reportDayAbsencesByUser.put(userIdComposite, reportDayAbsences);
            reportDayEntriesByUser.putIfAbsent(userIdComposite, List.of());
        }

        return new ReportDay(date, workingTimeCalendars, reportDayEntriesForDate.apply(date), reportDayAbsencesByUser);
    }

    private static boolean isPreviousMonth(LocalDate possiblePreviousMonthDate, YearMonth yearMonth) {
        return YearMonth.from(possiblePreviousMonthDate).until(yearMonth, MONTHS) == 1;
    }
}
