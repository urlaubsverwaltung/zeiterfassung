package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.settings.SubtractBreakFromTimeEntrySettings;
import de.focusshift.zeiterfassung.settings.SubtractBreakFromTimeEntrySettingsService;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
import de.focusshift.zeiterfassung.workduration.WorkDurationCalculationService;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendarService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
class TimeEntryDayServiceImpl implements TimeEntryDayService {

    private final TimeEntryService timeEntryService;
    private final TimeEntryLockService timeEntryLockService;
    private final WorkDurationCalculationService workDurationCalculator;
    private final WorkingTimeCalendarService workingTimeCalendarService;
    private final UserManagementService userManagementService;
    private final UserSettingsProvider userSettingsProvider;
    private final UserDateService userDateService;
    private final SubtractBreakFromTimeEntrySettingsService subtractBreakFromTimeEntrySettingsService;
    private final TimeEntryRepository timeEntryRepository;

    TimeEntryDayServiceImpl(
        TimeEntryService timeEntryService,
        TimeEntryLockService timeEntryLockService,
        WorkDurationCalculationService workDurationCalculator,
        WorkingTimeCalendarService workingTimeCalendarService,
        UserManagementService userManagementService,
        UserSettingsProvider userSettingsProvider,
        UserDateService userDateService,
        SubtractBreakFromTimeEntrySettingsService subtractBreakFromTimeEntrySettingsService,
        TimeEntryRepository timeEntryRepository
    ) {
        this.timeEntryService = timeEntryService;
        this.timeEntryLockService = timeEntryLockService;
        this.workDurationCalculator = workDurationCalculator;
        this.workingTimeCalendarService = workingTimeCalendarService;
        this.userManagementService = userManagementService;
        this.userSettingsProvider = userSettingsProvider;
        this.userDateService = userDateService;
        this.subtractBreakFromTimeEntrySettingsService = subtractBreakFromTimeEntrySettingsService;
        this.timeEntryRepository = timeEntryRepository;
    }

    @Override
    public List<TimeEntryDay> getTimeEntryDays(LocalDate from, LocalDate toExclusive, UserLocalId userLocalId) {
        final ZoneId zoneIdPivot = userSettingsProvider.zoneId();
        return getTimeEntryDays(from, toExclusive, zoneIdPivot, userLocalId);
    }

    @Override
    public Map<UserIdComposite, List<TimeEntryDay>> getTimeEntryDays(LocalDate from, LocalDate toExclusive, Collection<UserLocalId> userLocalIds) {

        final ZoneId zoneIdPivot = userSettingsProvider.zoneId();

        final Map<UserIdComposite, List<TimeEntry>> allTimeEntriesByUser = timeEntryService.getEntries(from, toExclusive, userLocalIds);
        final Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendarByUser = workingTimeCalendarService.getWorkingTimeCalendarForUsers(from, toExclusive, userLocalIds);

        return toTimeEntryDaysByUser(from, toExclusive, allTimeEntriesByUser, workingTimeCalendarByUser, zoneIdPivot);
    }

    @Override
    public Map<UserIdComposite, List<TimeEntryDay>> getTimeEntryDaysForAllUsers(LocalDate from, LocalDate toExclusive) {

        final ZoneId zoneIdPivot = userSettingsProvider.zoneId();

        final Map<UserIdComposite, List<TimeEntry>> allTimeEntriesByUser = timeEntryService.getEntriesForAllUsers(from, toExclusive);
        final Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendarByUser = workingTimeCalendarService.getWorkingTimeCalendarForAllUsers(from, toExclusive);

        return toTimeEntryDaysByUser(from, toExclusive, allTimeEntriesByUser, workingTimeCalendarByUser, zoneIdPivot);
    }

    @Override
    public TimeEntryWeekPage getEntryWeekPage(UserLocalId userLocalId, int year, int weekOfYear) {

        final ZoneId zoneIdPivot = userSettingsProvider.zoneId();

        final User user = findUser(userLocalId);
        final UserId userId = user.userIdComposite().id();

        final ZonedDateTime fromDateTime = userDateService.firstDayOfWeek(Year.of(year), weekOfYear).atStartOfDay(zoneIdPivot);
        final ZonedDateTime toDateTimeExclusive = fromDateTime.plusWeeks(1);
        final LocalDate fromLocalDate = LocalDate.ofInstant(fromDateTime.toInstant(), zoneIdPivot);
        final LocalDate toLocalDateExclusive = LocalDate.ofInstant(toDateTimeExclusive.toInstant(), zoneIdPivot);

        final List<TimeEntryDay> timeEntryDays = getTimeEntryDays(fromLocalDate, toLocalDateExclusive, zoneIdPivot, userLocalId);

        final PlannedWorkingHours planned = timeEntryDays.stream()
            .map(TimeEntryDay::plannedWorkingHours)
            .reduce(PlannedWorkingHours.ZERO, PlannedWorkingHours::plus);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(fromLocalDate, planned, timeEntryDays);
        final long totalTimeEntries = timeEntryRepository.countAllByOwner(userId.value());

        return new TimeEntryWeekPage(timeEntryWeek, totalTimeEntries);
    }

    private List<TimeEntryDay> getTimeEntryDays(LocalDate from, LocalDate toExclusive, ZoneId zoneIdPivot, UserLocalId userLocalId) {

        final List<TimeEntry> allTimeEntries = timeEntryService.getEntries(from, toExclusive, userLocalId);

        final Map<LocalDate, List<TimeEntry>> byDate = allTimeEntries.stream()
            .collect(groupingBy(timeEntry -> timeEntry.start().toLocalDate()));

        final WorkingTimeCalendar workingTimeCalender =
            workingTimeCalendarService.getWorkingTimeCalender(from, toExclusive, userLocalId);

        return createTimeEntryDays(from, toExclusive, byDate, workingTimeCalender, zoneIdPivot);
    }

    private User findUser(UserLocalId userLocalId) {
        return userManagementService.findUserByLocalId(userLocalId)
            .orElseThrow(() -> new IllegalStateException("expected user localId=%s to exist but got nothing.".formatted(userLocalId)));
    }

    private Map<UserIdComposite, List<TimeEntryDay>> toTimeEntryDaysByUser(
        LocalDate from, LocalDate toExclusive,
        Map<UserIdComposite, List<TimeEntry>> timeEntriesByUser,
        Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendarByUser,
        ZoneId zoneIdPivot
    ) {
        return timeEntriesByUser.entrySet().stream().collect(toMap(
            Map.Entry::getKey,
            entry -> {
                final UserIdComposite userIdComposite = entry.getKey();
                final Map<LocalDate, List<TimeEntry>> timeEntriesByDate = groupByDate(entry.getValue(), zoneIdPivot);
                final WorkingTimeCalendar workingTimeCalendar = workingTimeCalendarByUser.get(userIdComposite);
                return createTimeEntryDays(from, toExclusive, timeEntriesByDate, workingTimeCalendar, zoneIdPivot);
            }
        ));
    }

    private Map<LocalDate, List<TimeEntry>> groupByDate(List<TimeEntry> timeEntries, ZoneId zoneIdPivot) {
        return timeEntries.stream().collect(groupingBy(timeEntry -> timeEntry.start().withZoneSameInstant(zoneIdPivot).toLocalDate()));
    }

    private List<TimeEntryDay> createTimeEntryDays(LocalDate from, LocalDate toExclusive,
                                                   Map<LocalDate, List<TimeEntry>> timeEntriesByDate,
                                                   WorkingTimeCalendar workingTimeCalendar,
                                                   ZoneId zoneIdPivot) {
        // TODO inject me
        final Optional<LocalDate> minValidTimeEntryDate = timeEntryLockService.getMinValidTimeEntryDate(zoneIdPivot);

        // TODO inject me
        final Optional<SubtractBreakFromTimeEntrySettings> subtractBreakFromTimeEntrySettings =
            subtractBreakFromTimeEntrySettingsService.getSubtractBreakFromTimeEntrySettings();

        final List<TimeEntryDay> timeEntryDays = new ArrayList<>();

        // iterate from end to start -> last entry should be on top of the list (the first element)
        LocalDate date = toExclusive.minusDays(1);

        while (date.isEqual(from) || date.isAfter(from)) {

            final PlannedWorkingHours plannedWorkingHours = workingTimeCalendar.plannedWorkingHours(date)
                .orElseThrow(() -> new IllegalStateException("expected plannedWorkingHours to exist in calendar."));

            final boolean locked = isDateLocked(date, minValidTimeEntryDate);

            final List<TimeEntry> timeEntries = timeEntriesByDate.getOrDefault(date, List.of());
            final List<Absence> absences = workingTimeCalendar.absence(date).orElse(List.of());
            final ShouldWorkingHours shouldWorkingHours = workingTimeCalendar.shouldWorkingHours(date).orElse(ShouldWorkingHours.ZERO);

            final WorkDuration workDuration = subtractBreakFromTimeEntrySettings
                .map(settings -> workDurationCalculator.calculateWorkDuration(settings, timeEntries))
                .orElseGet(() -> workDurationCalculator.calculateWorkDuration(timeEntries));

            timeEntryDays.add(new TimeEntryDay(locked, date, workDuration, plannedWorkingHours, shouldWorkingHours, timeEntries, absences));

            date = date.minusDays(1);
        }

        return timeEntryDays;
    }

    private static boolean isDateLocked(LocalDate date, Optional<LocalDate> minValidTimeEntryDate) {
        return minValidTimeEntryDate.map(date::isBefore).orElse(false);
    }
}
