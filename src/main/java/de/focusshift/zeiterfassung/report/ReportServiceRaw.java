package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.settings.LockTimeEntriesSettings;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryDay;
import de.focusshift.zeiterfassung.timeentry.TimeEntryDayService;
import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.lang.invoke.MethodHandles.lookup;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.util.HashMap.newHashMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Service
public class ReportServiceRaw {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final TimeEntryDayService timeEntryDayService;
    private final UserManagementService userManagementService;
    private final UserDateService userDateService;
    private final WorkingTimeCalendarService workingTimeCalendarService;
    private final TimeEntryLockService timeEntryLockService;

    ReportServiceRaw(
        TimeEntryDayService timeEntryDayService,
        UserManagementService userManagementService,
        UserDateService userDateService,
        WorkingTimeCalendarService workingTimeCalendarService,
        TimeEntryLockService timeEntryLockService
    ) {

        this.timeEntryDayService = timeEntryDayService;
        this.userManagementService = userManagementService;
        this.userDateService = userDateService;
        this.workingTimeCalendarService = workingTimeCalendarService;
        this.timeEntryLockService = timeEntryLockService;
    }

    public ReportDay getReportDayForAllUsers(LocalDate date) {

        final Map<UserIdComposite, User> userById = userManagementService.findAllUsers().stream()
            .collect(toMap(User::userIdComposite, identity()));

        final Map<LocalDate, Map<UserIdComposite, TimeEntryDay>> timeEntryDaysByDate =
            timeEntryDaysForAllUsers(date, date.plusDays(1));

        final Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendarByUserId =
            workingTimeCalendarService.getWorkingTimeCalendarForAllUsers(date, date.plusDays(1));

        final boolean locked = timeEntryLockService.isLocked(date);

        return toReportDay(date, userById, workingTimeCalendarByUserId, timeEntryDaysByDate, locked);
    }

    ReportWeek getReportWeek(Year year, int week, UserLocalId userLocalId) {

        final User user = userManagementService.findUserByLocalId(userLocalId)
            .orElseThrow(() -> new IllegalStateException("could not find user=%s".formatted(userLocalId)));

        return createReportWeek(year, week,
            List.of(user),
            period -> Map.of(user.userIdComposite(), timeEntryDayService.getTimeEntryDays(period.from(), period.toExclusive(), userLocalId)),
            period -> workingTimeCalendarService.getWorkingTimeCalendarForUsers(period.from(), period.toExclusive(), List.of(user.userLocalId())));
    }

    ReportWeek getReportWeek(Year year, int week, List<UserLocalId> userLocalIds) {

        final List<User> users = userManagementService.findAllUsersByLocalIds(userLocalIds);

        return createReportWeek(year, week,
            users,
            period -> timeEntryDayService.getTimeEntryDays(period.from(), period.toExclusive(), userLocalIds),
            period -> workingTimeCalendarService.getWorkingTimeCalendarForUsers(period.from(), period.toExclusive(), userLocalIds));
    }

    ReportWeek getReportWeekForAllUsers(Year year, int week) {

        final List<User> users = userManagementService.findAllUsers();

        return createReportWeek(year, week,
            users,
            period -> timeEntryDayService.getTimeEntryDaysForAllUsers(period.from(), period.toExclusive()),
            period -> workingTimeCalendarService.getWorkingTimeCalendarForAllUsers(period.from(), period.toExclusive()));
    }

    ReportMonth getReportMonth(YearMonth yearMonth, UserId userId) {

        final User user = userManagementService.findUserById(userId)
            .orElseThrow(() -> new IllegalStateException("could not find user id=%s".formatted(userId)));

        return createReportMonth(yearMonth,
            List.of(user),
            period -> timeEntryDayService.getTimeEntryDays(period.from(), period.toExclusive(), List.of(user.userLocalId())),
            period -> workingTimeCalendarService.getWorkingTimeCalendarForUsers(period.from(), period.toExclusive(), List.of(user.userLocalId())));
    }

    ReportMonth getReportMonth(YearMonth yearMonth, List<UserLocalId> userLocalIds) {

        final List<User> users = userManagementService.findAllUsersByLocalIds(userLocalIds);

        return createReportMonth(yearMonth,
            users,
            period -> timeEntryDayService.getTimeEntryDays(period.from(), period.toExclusive(), userLocalIds),
            period -> workingTimeCalendarService.getWorkingTimeCalendarForUsers(period.from(), period.toExclusive(), userLocalIds));
    }

    ReportMonth getReportMonthForAllUsers(YearMonth yearMonth) {

        final List<User> users = userManagementService.findAllUsers();

        return createReportMonth(yearMonth,
            users,
            period -> timeEntryDayService.getTimeEntryDaysForAllUsers(period.from(), period.toExclusive()),
            period -> workingTimeCalendarService.getWorkingTimeCalendarForAllUsers(period.from(), period.toExclusive()));
    }

    private Map<LocalDate, Map<UserIdComposite, TimeEntryDay>> timeEntryDaysForAllUsers(LocalDate from, LocalDate toExclusive) {
        final Map<UserIdComposite, List<TimeEntryDay>> timeEntryDaysByUser = timeEntryDayService.getTimeEntryDaysForAllUsers(from, toExclusive);
        return groupByDate(timeEntryDaysByUser);
    }

    private Map<LocalDate, Map<UserIdComposite, TimeEntryDay>> groupByDate(Map<UserIdComposite, List<TimeEntryDay>> daysByUser) {

        final Map<LocalDate, Map<UserIdComposite, TimeEntryDay>> byDate = new HashMap<>();

        for (Map.Entry<UserIdComposite, List<TimeEntryDay>> entry : daysByUser.entrySet()) {
            final UserIdComposite userId = entry.getKey();
            for (TimeEntryDay timeEntryDay : entry.getValue()) {
                final Map<UserIdComposite, TimeEntryDay> map = byDate.computeIfAbsent(timeEntryDay.date(), date -> new HashMap<>());
                map.put(userId, timeEntryDay);
            }
        }

        return byDate;
    }

    private ReportWeek createReportWeek(Year year, int week,
                                        List<User> users,
                                        Function<Period, Map<UserIdComposite, List<TimeEntryDay>>> timeEntryDaysProvider,
                                        Function<Period, Map<UserIdComposite, WorkingTimeCalendar>> workingTimeCalendarProvider) {

        final LocalDate firstDateOfWeek = userDateService.firstDayOfWeek(year, week);
        final Period period = new Period(firstDateOfWeek, firstDateOfWeek.plusWeeks(1));

        final Map<UserIdComposite, List<TimeEntryDay>> timeEntryDays = timeEntryDaysProvider.apply(period);
        final Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendars = workingTimeCalendarProvider.apply(period);

        return reportWeek(firstDateOfWeek, users, timeEntryDays, workingTimeCalendars);
    }

    private ReportMonth createReportMonth(YearMonth yearMonth,
                                          List<User> users,
                                          Function<Period, Map<UserIdComposite, List<TimeEntryDay>>> timeEntryDaysProvider,
                                          Function<Period, Map<UserIdComposite, WorkingTimeCalendar>> workingTimeCalendarProvider) {

        final LocalDate firstOfMonth = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), 1);

        final Period period = new Period(firstOfMonth, firstOfMonth.plusMonths(1));

        final Map<UserIdComposite, List<TimeEntryDay>> timeEntryDays = timeEntryDaysProvider.apply(period);
        final Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendars = workingTimeCalendarProvider.apply(period);

        final List<ReportWeek> weeks = getStartOfWeekDatesForMonth(yearMonth)
            .stream()
            .map(startOfWeekDate -> reportWeek(startOfWeekDate, users, timeEntryDays, workingTimeCalendars))
            .toList();

        return new ReportMonth(yearMonth, weeks);
    }

    private ReportWeek reportWeek(LocalDate startOfWeekDate, List<User> users,
                                  Map<UserIdComposite, List<TimeEntryDay>> timeEntryDaysByUser,
                                  Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendars) {

        final Map<UserIdComposite, User> userById = users.stream().collect(toMap(User::userIdComposite, identity()));
        final Map<LocalDate, Map<UserIdComposite, TimeEntryDay>> timeEntryByDate = groupByDate(timeEntryDaysByUser);

        final LockTimeEntriesSettings settings = timeEntryLockService.getLockTimeEntriesSettings();

        final List<ReportDay> reportDays = IntStream.rangeClosed(0, 6)
            .mapToObj(daysToAdd -> {
                final LocalDate date = startOfWeekDate.plusDays(daysToAdd);
                final boolean locked = timeEntryLockService.isLocked(date, settings);
                return toReportDay(
                    date,
                    userById,
                    workingTimeCalendars,
                    timeEntryByDate,
                    locked
                );
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

    record Period(LocalDate from, LocalDate toExclusive) {
    }

    private static Optional<ReportDayEntry> timeEntryToReportDayEntry(TimeEntry timeEntry, Function<UserIdComposite, User> userProvider) {

        final UserIdComposite userId = timeEntry.userIdComposite();
        final User user = userProvider.apply(userId);

        if (user == null) {
            LOG.info("could not find user with id={} for timeEntry={} while generating report.", userId, timeEntry.id());
            return Optional.empty();
        }

        return Optional.of(new ReportDayEntry(
            timeEntry.id(),
            user,
            timeEntry.comment(),
            timeEntry.start(),
            timeEntry.end(),
            timeEntry.workDuration(),
            timeEntry.isBreak()
        ));
    }

    private static ReportDay toReportDay(LocalDate date,
                                         Map<UserIdComposite, User> userById,
                                         Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendars,
                                         Map<LocalDate, Map<UserIdComposite, TimeEntryDay>> timeEntryDayByDate,
                                         boolean dateIsLocked) {

        final Map<UserIdComposite, TimeEntryDay> timeEntryDayByUser = timeEntryDayByDate.getOrDefault(date, newHashMap(userById.size()));
        final Map<UserIdComposite, List<ReportDayEntry>> reportDayEntriesByUser = newHashMap(userById.size());
        final Map<UserIdComposite, WorkDuration> workDurationByUser = newHashMap(userById.size());
        final Map<UserIdComposite, List<ReportDayAbsence>> reportDayAbsencesByUser = newHashMap(userById.size());

        for (Map.Entry<UserIdComposite, User> entry : userById.entrySet()) {
            final UserIdComposite userIdComposite = entry.getKey();
            final User user = entry.getValue();

            final TimeEntryDay timeEntryDay = timeEntryDayByUser.get(userIdComposite);
            if (timeEntryDay == null) {
                reportDayEntriesByUser.put(userIdComposite, List.of());
                workDurationByUser.put(userIdComposite, WorkDuration.ZERO);
            } else {
                final List<ReportDayEntry> reportDayEntries = timeEntryDay.timeEntries().stream()
                    .map(timeEntry -> timeEntryToReportDayEntry(timeEntry, userById::get))
                    .flatMap(Optional::stream)
                    .toList();
                reportDayEntriesByUser.put(userIdComposite, reportDayEntries);
                workDurationByUser.put(userIdComposite, timeEntryDay.workDuration());
            }

            final WorkingTimeCalendar workingTimeCalendar = workingTimeCalendars.get(userIdComposite);
            if (workingTimeCalendar == null) {
                LOG.warn("could not find workingTimeCalendar for userId={} while generating reportDay date={}. Falling back to empty list of absences.", userIdComposite, date);
                reportDayAbsencesByUser.put(userIdComposite, List.of());
            } else {
                final List<Absence> absences = workingTimeCalendar.absence(date).orElse(List.of());
                final List<ReportDayAbsence> reportDayAbsences = absences.stream().map(absence -> new ReportDayAbsence(user, absence)).toList();
                reportDayAbsencesByUser.put(userIdComposite, reportDayAbsences);
            }
        }

        return new ReportDay(date, dateIsLocked, workingTimeCalendars, reportDayEntriesByUser, workDurationByUser, reportDayAbsencesByUser);
    }

    private static boolean isPreviousMonth(LocalDate possiblePreviousMonthDate, YearMonth yearMonth) {
        return YearMonth.from(possiblePreviousMonthDate).until(yearMonth, MONTHS) == 1;
    }
}
