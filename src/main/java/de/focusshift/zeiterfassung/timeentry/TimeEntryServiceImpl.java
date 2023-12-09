package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.AbsenceService;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.usermanagement.WorkingTimeCalendar;
import de.focusshift.zeiterfassung.usermanagement.WorkingTimeCalendarService;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
class TimeEntryServiceImpl implements TimeEntryService {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    public static final BigDecimal ONE_MINUTE_IN_SECONDS = BigDecimal.valueOf(60);

    private final TimeEntryRepository timeEntryRepository;
    private final UserManagementService userManagementService;
    private final WorkingTimeCalendarService workingTimeCalendarService;
    private final UserDateService userDateService;
    private final UserSettingsProvider userSettingsProvider;
    private final AbsenceService absenceService;
    private final Clock clock;

    @Autowired
    TimeEntryServiceImpl(TimeEntryRepository timeEntryRepository, UserManagementService userManagementService,
                         WorkingTimeCalendarService workingTimeCalendarService, UserDateService userDateService,
                         UserSettingsProvider userSettingsProvider, AbsenceService absenceService, Clock clock) {

        this.timeEntryRepository = timeEntryRepository;
        this.userManagementService = userManagementService;
        this.workingTimeCalendarService = workingTimeCalendarService;
        this.userDateService = userDateService;
        this.userSettingsProvider = userSettingsProvider;
        this.absenceService = absenceService;
        this.clock = clock;
    }

    @Override
    public Optional<TimeEntry> findTimeEntry(long id) {
        return timeEntryRepository.findById(id).map(this::toTimeEntry);
    }

    @Override
    public List<TimeEntry> getEntries(LocalDate from, LocalDate toExclusive, UserId userId) {

        final User user = findUser(userId);
        final Instant fromInstant = toInstant(from);
        final Instant toInstant = toInstant(toExclusive);

        final List<TimeEntryEntity> result = timeEntryRepository
            .findAllByOwnerAndStartGreaterThanEqualAndStartLessThan(userId.value(), fromInstant, toInstant);

        return result
            .stream()
            .map(timeEntryEntity -> toTimeEntry(timeEntryEntity, user))
            .sorted(comparing(TimeEntry::start).reversed())
            .toList();
    }

    @Override
    public Map<UserIdComposite, List<TimeEntry>> getEntriesForAllUsers(LocalDate from, LocalDate toExclusive) {

        final Instant fromInstant = toInstant(from);
        final Instant toInstant = toInstant(toExclusive);

        final Map<UserId, User> userByUserId = userManagementService.findAllUsers()
            .stream()
            .collect(toMap(User::userId, identity()));

        return timeEntryRepository.findAllByStartGreaterThanEqualAndStartLessThan(fromInstant, toInstant)
            .stream()
            .map(timeEntryEntity -> toTimeEntry(timeEntryEntity, userByUserId))
            .filter(Objects::nonNull)
            .collect(groupingBy(TimeEntry::userIdComposite));
    }

    @Override
    public Map<UserIdComposite, List<TimeEntry>> getEntriesByUserLocalIds(LocalDate from, LocalDate toExclusive, List<UserLocalId> userLocalIds) {

        final Instant fromInstant = toInstant(from);
        final Instant toInstant = toInstant(toExclusive);

        final List<User> users = userManagementService.findAllUsersByLocalIds(userLocalIds);

        final List<String> userIdValues = new ArrayList<>();
        final Map<UserId, User> userByUserId = new HashMap<>();
        for (User user : users) {
            userIdValues.add(user.userIdComposite().id().value());
            userByUserId.put(user.userId(), user);
        }

        final Map<UserIdComposite, List<TimeEntry>> result = timeEntryRepository
            .findAllByOwnerIsInAndStartGreaterThanEqualAndStartLessThan(userIdValues, fromInstant, toInstant)
            .stream()
            .map(timeEntryEntity -> toTimeEntry(timeEntryEntity, userByUserId))
            .filter(Objects::nonNull)
            .collect(groupingBy(TimeEntry::userIdComposite));

        // add empty list for users without time entries
        users.forEach(user -> result.computeIfAbsent(user.userIdComposite(), unused -> List.of()));

        return result;
    }

    @Override
    public TimeEntryWeekPage getEntryWeekPage(UserId userId, int year, int weekOfYear) {

        final User user = findUser(userId);
        final ZoneId userZoneId = userSettingsProvider.zoneId();
        final ZonedDateTime fromDateTime = userDateService.firstDayOfWeek(Year.of(year), weekOfYear).atStartOfDay(userZoneId);
        final ZonedDateTime toDateTimeExclusive = fromDateTime.plusWeeks(1);
        final LocalDate fromLocalDate = LocalDate.ofInstant(fromDateTime.toInstant(), userZoneId);
        final LocalDate toLocalDateExclusive = LocalDate.ofInstant(toDateTimeExclusive.toInstant(), userZoneId);
        final Instant from = Instant.from(fromDateTime);
        final Instant toExclusive = Instant.from(toDateTimeExclusive);

        final Map<LocalDate, List<TimeEntry>> timeEntriesByDate = timeEntryRepository.findAllByOwnerAndStartGreaterThanEqualAndStartLessThan(userId.value(), from, toExclusive)
            .stream()
            .sorted(comparing(TimeEntryEntity::getStart).thenComparing(TimeEntryEntity::getUpdatedAt).reversed())
            .map(timeEntryEntity -> toTimeEntry(timeEntryEntity, user))
            .collect(groupingBy(entry -> LocalDate.ofInstant(entry.start().toInstant(), userZoneId)));

        final Map<LocalDate, List<Absence>> absencesByDate = absenceService.findAllAbsences(userId, from, toExclusive);

        // TODO refactor getEntryWeekPage to accept UserLocalId to replace userManagementService call
        final UserLocalId userLocalId = user.userLocalId();

        final WorkingTimeCalendar workingTimeCalendar = workingTimeCalendarService
            .getWorkingTimes(fromLocalDate, toLocalDateExclusive, userLocalId);

        final List<TimeEntryDay> daysOfWeek = createTimeEntryDays(fromLocalDate, toLocalDateExclusive, timeEntriesByDate, absencesByDate, workingTimeCalendar);

        final PlannedWorkingHours weekPlannedHours = workingTimeCalendar
            .plannedWorkingHours(fromLocalDate, toLocalDateExclusive);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(fromLocalDate, weekPlannedHours, daysOfWeek);
        final long totalTimeEntries = timeEntryRepository.countAllByOwner(userId.value());

        return new TimeEntryWeekPage(timeEntryWeek, totalTimeEntries);
    }

    @Override
    public TimeEntry createTimeEntry(UserId userId, String comment, ZonedDateTime start, ZonedDateTime end, boolean isBreak) {

        final TimeEntryEntity entity = new TimeEntryEntity();
        entity.setOwner(userId.value());
        entity.setComment(comment.strip());
        entity.setStart(start.toInstant());
        entity.setStartZoneId(start.getZone().getId());
        entity.setEnd(end.toInstant());
        entity.setEndZoneId(end.getZone().getId());
        entity.setBreak(isBreak);

        return save(entity);
    }

    @Override
    public TimeEntry updateTimeEntry(TimeEntryId id, String comment, @Nullable ZonedDateTime start, @Nullable ZonedDateTime end,
                                     @Nullable Duration duration, boolean isBreak) throws TimeEntryUpdateNotPlausibleException {

        final TimeEntryEntity entity = timeEntryRepository.findById(id.value())
            .orElseThrow(() -> new IllegalStateException("could not find existing timeEntry id=%s".formatted(id)));

        updateEntityTimeSpan(entity, start, end, duration);

        entity.setComment(comment.strip());
        entity.setBreak(isBreak);

        return save(entity);
    }

    private static List<TimeEntryDay> createTimeEntryDays(LocalDate from, LocalDate toExclusive,
                                                          Map<LocalDate, List<TimeEntry>> timeEntriesByDate,
                                                          Map<LocalDate, List<Absence>> absencesByDate,
                                                          WorkingTimeCalendar workingTimeCalendar) {

        final List<TimeEntryDay> timeEntryDays = new ArrayList<>();

        // iterate from end to start -> last entry should be on top of the list (the first element)
        LocalDate date = toExclusive.minusDays(1);

        while (date.isEqual(from) || date.isAfter(from)) {

            final PlannedWorkingHours plannedWorkingHours = workingTimeCalendar.plannedWorkingHours(date)
                .orElseThrow(() -> new IllegalStateException("expected plannedWorkingHours to exist in calendar."));

            final List<TimeEntry> timeEntries = timeEntriesByDate.getOrDefault(date, List.of());
            final List<Absence> absences = absencesByDate.getOrDefault(date, List.of());
            final ShouldWorkingHours shouldWorkingHours = dayShouldHoursWorked(plannedWorkingHours, absences);
            timeEntryDays.add(new TimeEntryDay(date, plannedWorkingHours, shouldWorkingHours, timeEntries, absences));

            date = date.minusDays(1);
        }

        return timeEntryDays;
    }

    private static ShouldWorkingHours dayShouldHoursWorked(PlannedWorkingHours plannedWorkingHours, List<Absence> absences) {

        final double absenceDayLengthValue = absences.stream()
            .map(Absence::dayLength)
            .map(DayLength::getValue)
            .reduce(0.0, Double::sum);

        if (absenceDayLengthValue >= 1.0) {
            return ShouldWorkingHours.ZERO;
        } else if (absenceDayLengthValue == 0.5) {
            return new ShouldWorkingHours(plannedWorkingHours.duration().dividedBy(2));
        }

        return new ShouldWorkingHours(plannedWorkingHours.duration());

    }

    private void updateEntityTimeSpan(TimeEntryEntity entity, ZonedDateTime start, ZonedDateTime end, Duration duration)
        throws TimeEntryUpdateNotPlausibleException {

        final TimeEntry existingTimeEntry = toTimeEntry(entity);

        // start + end + duration (check if it is plausible)
        if (start != null && end != null && duration != null) {
            final boolean startChanged = notEquals(existingTimeEntry.start(), start);
            final boolean endChanged = notEquals(existingTimeEntry.end(), end);
            final boolean durationChanged = !duration.equals(Duration.ZERO) && !existingTimeEntry.duration().equals(duration);
            final boolean plausibleUpdate = duration.equals(Duration.ZERO) || delta(start, end).equals(duration);

            if (startChanged && endChanged && durationChanged && !plausibleUpdate) {
                throw new TimeEntryUpdateNotPlausibleException("cannot update time-entry when start, end and duration should be changed. pick two of them.");
            }
            if (plausibleUpdate || startChanged) {
                setStart(entity, start);
                if (durationChanged) {
                    setEnd(entity, start.plusMinutes(duration.toMinutes()));
                }
            }
            if (plausibleUpdate || endChanged) {
                setEnd(entity, end);
                if (durationChanged) {
                    setStart(entity, end.minusMinutes(duration.toMinutes()));
                }
            }
            if (durationChanged && !startChanged && !endChanged) {
                setEnd(entity, start.plusMinutes(duration.toMinutes()));
            }
        }
        // start + end
        else if (start != null && end != null) {
            setStart(entity, start);
            setEnd(entity, end);
        }
        // start + duration
        else if (start != null && duration != null) {
            setStart(entity, start);
            setEnd(entity, start.plusMinutes(duration.toMinutes()));
        }
        // end + duration
        else if (start == null && end != null && duration != null) {
            setStart(entity, end.minusMinutes(duration.toMinutes()));
            setEnd(entity, end);
        }
    }

    private static void setStart(TimeEntryEntity entity, ZonedDateTime start) {
        entity.setStart(start.toInstant());
        entity.setStartZoneId(start.getZone().getId());
    }
    private static void setEnd(TimeEntryEntity entity, ZonedDateTime end) {
        entity.setEnd(end.toInstant());
        entity.setEndZoneId(end.getZone().getId());
    }

    private static Duration delta(ZonedDateTime first, ZonedDateTime second) {
        return Duration.ofMinutes(MINUTES.between(first, second));
    }

    private static boolean notEquals(ZonedDateTime one, ZonedDateTime two) {
        return !one.toInstant().atZone(UTC).equals(two.toInstant().atZone(UTC));
    }

    @Override
    public void deleteTimeEntry(long timeEntryId) {
        timeEntryRepository.deleteById(timeEntryId);
    }

    private TimeEntry save(TimeEntryEntity entity) {
        entity.setUpdatedAt(Instant.now(clock));
        return toTimeEntry(timeEntryRepository.save(entity));
    }

    private User findUser(UserId userId) {
        return userManagementService.findUserById(userId)
            .orElseThrow(() -> new IllegalStateException("expected user=%s to exist but got nothing.".formatted(userId)));
    }

    private TimeEntry toTimeEntry(TimeEntryEntity entity) {
        final User user = findUser(new UserId(entity.getOwner()));
        return toTimeEntry(entity, user);
    }

    @Nullable
    private TimeEntry toTimeEntry(TimeEntryEntity timeEntryEntity, Map<UserId, User> userByUserId) {
        final UserId userId = new UserId(timeEntryEntity.getOwner());
        final User user = userByUserId.get(userId);
        if (user == null) {
            LOG.info("cannot map TimeEntryEntity with user={} because user does not exist anymore.", userId);
            LOG.info("ignoring {}", timeEntryEntity);
            return null;
        }
        return toTimeEntry(timeEntryEntity, user);
    }

    private TimeEntry toTimeEntry(TimeEntryEntity entity, User user) {

        final Instant actualStart = entity.getStart();
        final Instant actualEnd = entity.getEnd();

        final BigDecimal minutesDelta = BigDecimal.valueOf(actualStart.until(actualEnd, SECONDS))
            .divide(ONE_MINUTE_IN_SECONDS, RoundingMode.UP);

        final Instant start = actualStart.truncatedTo(MINUTES);
        final Instant end = start.plus(minutesDelta.longValue(), MINUTES);

        final ZonedDateTime startDateTime = ZonedDateTime.ofInstant(start, ZoneId.of(entity.getStartZoneId()));
        final ZonedDateTime endDateTime = ZonedDateTime.ofInstant(end, ZoneId.of(entity.getEndZoneId()));

        final UserIdComposite userIdComposite = user.userIdComposite();

        return new TimeEntry(new TimeEntryId(entity.getId()), userIdComposite, entity.getComment(), startDateTime, endDateTime, entity.isBreak());
    }

    private static Instant toInstant(LocalDate localDate) {
        return localDate.atStartOfDay(UTC).toInstant();
    }
}
