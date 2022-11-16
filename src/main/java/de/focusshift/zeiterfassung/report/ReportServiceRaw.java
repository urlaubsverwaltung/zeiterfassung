package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.invoke.MethodHandles.lookup;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.util.Collections.emptyList;
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

    ReportServiceRaw(TimeEntryService timeEntryService, UserManagementService userManagementService, UserDateService userDateService) {
        this.timeEntryService = timeEntryService;
        this.userManagementService = userManagementService;
        this.userDateService = userDateService;
    }

    ReportWeek getReportWeek(Year year, int week, UserId userId) {
        return createReportWeek(year, week,
            period -> timeEntryService.getEntries(period.from(), period.toExclusive(), userId));
    }

    ReportWeek getReportWeek(Year year, int week, List<UserLocalId> userLocalIds) {
        return createReportWeek(year, week,
            period -> timeEntryService.getEntriesByUserLocalIds(period.from(), period.toExclusive(), userLocalIds));
    }

    ReportWeek getReportWeekForAllUsers(Year year, int week) {
        return createReportWeek(year, week,
            period -> timeEntryService.getEntriesForAllUsers(period.from(), period.toExclusive()));
    }

    ReportMonth getReportMonth(YearMonth yearMonth, UserId userId) {
        return createReportMonth(yearMonth,
            period -> timeEntryService.getEntries(period.from(), period.toExclusive(), userId));
    }

    ReportMonth getReportMonth(YearMonth yearMonth, List<UserLocalId> userLocalIds) {
        return createReportMonth(yearMonth,
            period -> timeEntryService.getEntriesByUserLocalIds(period.from(), period.toExclusive(), userLocalIds));
    }

    ReportMonth getReportMonthForAllUsers(YearMonth yearMonth) {
        return createReportMonth(yearMonth,
            period -> timeEntryService.getEntriesForAllUsers(period.from(), period.toExclusive()));
    }

    private ReportWeek createReportWeek(Year year, int week, Function<Period, List<TimeEntry>> timeEntriesProvider) {

        final LocalDate firstDateOfWeek = userDateService.firstDayOfWeek(year, week);

        final Period period = new Period(firstDateOfWeek, firstDateOfWeek.plusWeeks(1));
        final List<TimeEntry> timeEntries = timeEntriesProvider.apply(period);

        final Map<UserId, User> userById = userByIdForTimeEntries(timeEntries);

        return reportWeek(firstDateOfWeek, timeEntries, userById);
    }

    private ReportMonth createReportMonth(YearMonth yearMonth, Function<Period, List<TimeEntry>> timeEntriesProvider) {

        final LocalDate firstOfMonth = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), 1);

        final Period period = new Period(firstOfMonth, firstOfMonth.plusMonths(1));
        final List<TimeEntry> timeEntries = timeEntriesProvider.apply(period);

        final Map<UserId, User> userById = userByIdForTimeEntries(timeEntries);

        List<ReportWeek> weeks = getStartOfWeekDatesForMonth(yearMonth)
            .stream()
            .map(startOfWeekDate -> reportWeek(startOfWeekDate, timeEntries, userById))
            .toList();

        return new ReportMonth(yearMonth, weeks);
    }

    private Map<UserId, User> userByIdForTimeEntries(List<TimeEntry> timeEntries) {
        final List<UserId> userIds = timeEntries.stream().map(TimeEntry::userId).distinct().toList();
        return userManagementService.findAllUsersByIds(userIds)
            .stream()
            .collect(toMap(User::id, Function.identity()));
    }

    private ReportWeek reportWeek(LocalDate startOfWeekDate, List<TimeEntry> timeEntries, Map<UserId, User> userById) {
        final Map<LocalDate, List<ReportDayEntry>> reportDayEntriesByDate = timeEntries
            .stream()
            .flatMap(timeEntry -> timeEntryToReportDayEntries(timeEntry, userById::get))
            .collect(groupingBy(reportDayEntry -> reportDayEntry.start().toLocalDate()));

        final Function<LocalDate, List<ReportDayEntry>> resolveReportDayEntries =
            (LocalDate date) -> reportDayEntriesByDate.getOrDefault(date, emptyList());

        final List<ReportDay> reportDays = IntStream.rangeClosed(0, 6)
            .mapToObj(daysToAdd -> toReportDay(startOfWeekDate.plusDays(daysToAdd), resolveReportDayEntries))
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

    private static Stream<ReportDayEntry> timeEntryToReportDayEntries(TimeEntry timeEntry, Function<UserId, User> userProvider) {

        final String comment = timeEntry.comment();
        final ZonedDateTime startDateTime = timeEntry.start();
        final ZonedDateTime endDateTime = timeEntry.end();

        final User user = userProvider.apply(timeEntry.userId());
        if (user == null) {
            LOG.info("could not find user with id={} for timeEntry={} while generating report.", timeEntry.userId(), timeEntry.id());
            return Stream.empty();
        }

        final ReportDayEntry first = new ReportDayEntry(user, comment, startDateTime, endDateTime);
        return Stream.of(first);
    }

    private static ReportDay toReportDay(LocalDate date, Function<LocalDate, List<ReportDayEntry>> resolveReportDayEntries) {
        return new ReportDay(date, resolveReportDayEntries.apply(date));
    }

    private static boolean isPreviousMonth(LocalDate possiblePreviousMonthDate, YearMonth yearMonth) {
        return YearMonth.from(possiblePreviousMonthDate).until(yearMonth, MONTHS) == 1;
    }
}
