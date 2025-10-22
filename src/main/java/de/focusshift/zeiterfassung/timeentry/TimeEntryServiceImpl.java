package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.data.history.EntityRevisionMapper;
import de.focusshift.zeiterfassung.data.history.EntityRevisionMetadata;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryCreatedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryDeletedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryUpdatedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryUpdatedEvent.UpdatedValueCandidate;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendarService;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
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
import java.util.function.Supplier;

import static java.lang.invoke.MethodHandles.lookup;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNullElse;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
class TimeEntryServiceImpl implements TimeEntryService {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    public static final BigDecimal ONE_MINUTE_IN_SECONDS = BigDecimal.valueOf(60);

    private final TimeEntryRepository timeEntryRepository;
    private final TimeEntryLockService timeEntryLockService;
    private final UserManagementService userManagementService;
    private final WorkingTimeCalendarService workingTimeCalendarService;
    private final UserDateService userDateService;
    private final UserSettingsProvider userSettingsProvider;
    private final EntityRevisionMapper entityRevisionMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    TimeEntryServiceImpl(
        TimeEntryRepository timeEntryRepository,
        TimeEntryLockService timeEntryLockService,
        UserManagementService userManagementService,
        WorkingTimeCalendarService workingTimeCalendarService,
        UserDateService userDateService,
        UserSettingsProvider userSettingsProvider,
        EntityRevisionMapper entityRevisionMapper,
        ApplicationEventPublisher applicationEventPublisher,
        Clock clock
    ) {

        this.timeEntryRepository = timeEntryRepository;
        this.timeEntryLockService = timeEntryLockService;
        this.userManagementService = userManagementService;
        this.workingTimeCalendarService = workingTimeCalendarService;
        this.userDateService = userDateService;
        this.userSettingsProvider = userSettingsProvider;
        this.entityRevisionMapper = entityRevisionMapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.clock = clock;
    }

    @Override
    public Optional<TimeEntry> findTimeEntry(TimeEntryId id) {
        return timeEntryRepository.findById(id.value()).map(this::toTimeEntry);
    }

    @Override
    public Optional<TimeEntryHistory> findTimeEntryHistory(TimeEntryId id) {

        final Revisions<Long, TimeEntryEntity> revisions = timeEntryRepository.findRevisions(id.value());
        if (revisions.isEmpty()) {
            LOG.warn("Could not find any revision for {}. A valid TimeEntry should have at least one revision of type INSERT.", id);
            return Optional.empty();
        }

        final UserId userId = new UserId(revisions.iterator().next().getEntity().getOwner());
        final User user = findUser(userId);

        final List<TimeEntryHistoryItem> historyItems = new ArrayList<>();
        TimeEntry previousTimeEntry = null;

        for (Revision<Long, TimeEntryEntity> revision : revisions.getContent()) {

            final TimeEntryEntity entity = revision.getEntity();
            final TimeEntry timeEntry = toTimeEntry(entity, user);
            final EntityRevisionMetadata entityRevisionMetadata = entityRevisionMapper.toEntityRevisionMetadata(revision);

            TimeEntryHistoryItem historyItem;

            if (previousTimeEntry == null) {
                historyItem = new TimeEntryHistoryItem(entityRevisionMetadata, timeEntry, true, true, true, true);
            } else {
                historyItem = new TimeEntryHistoryItem(
                    entityRevisionMetadata,
                    timeEntry,
                    hasBeenModified(previousTimeEntry::comment, timeEntry::comment),
                    hasBeenModified(previousTimeEntry::start, timeEntry::start),
                    hasBeenModified(previousTimeEntry::end, timeEntry::end),
                    hasBeenModified(previousTimeEntry::isBreak, timeEntry::isBreak)
                );
            }

            historyItems.add(historyItem);
            previousTimeEntry = timeEntry;
        }

        return Optional.of(new TimeEntryHistory(id, historyItems));
    }

    @Override
    public List<TimeEntry> getEntries(LocalDate from, LocalDate toExclusive, UserLocalId userLocalId) {

        final User user = findUser(userLocalId);
        final UserId userId = user.userIdComposite().id();

        final Instant fromInstant = toInstant(from);
        final Instant toInstant = toInstant(toExclusive);

        return timeEntryRepository
            .findAllByOwnerAndStartGreaterThanEqualAndStartLessThanOrderByStartDesc(userId.value(), fromInstant, toInstant).stream()
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

        return timeEntryRepository.findAllByStartGreaterThanEqualAndStartLessThanOrderByStartDesc(fromInstant, toInstant).stream()
            .map(timeEntryEntity -> toTimeEntry(timeEntryEntity, userByUserId))
            .filter(Objects::nonNull)
            .collect(groupingBy(TimeEntry::userIdComposite));
    }

    @Override
    public Map<UserIdComposite, List<TimeEntry>> getEntries(LocalDate from, LocalDate toExclusive, List<UserLocalId> userLocalIds) {

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
            .findAllByOwnerIsInAndStartGreaterThanEqualAndStartLessThanOrderByStartDesc(userIdValues, fromInstant, toInstant).stream()
            .map(timeEntryEntity -> toTimeEntry(timeEntryEntity, userByUserId))
            .filter(Objects::nonNull)
            .collect(groupingBy(TimeEntry::userIdComposite));

        // add empty list for users without time entries
        users.forEach(user -> result.computeIfAbsent(user.userIdComposite(), unused -> List.of()));

        return result;
    }

    @Override
    public TimeEntryWeekPage getEntryWeekPage(UserLocalId userLocalId, int year, int weekOfYear) {

        final User user = findUser(userLocalId);
        final UserId userId = user.userIdComposite().id();

        final ZoneId userZoneId = userSettingsProvider.zoneId();
        final ZonedDateTime fromDateTime = userDateService.firstDayOfWeek(Year.of(year), weekOfYear).atStartOfDay(userZoneId);
        final ZonedDateTime toDateTimeExclusive = fromDateTime.plusWeeks(1);
        final LocalDate fromLocalDate = LocalDate.ofInstant(fromDateTime.toInstant(), userZoneId);
        final LocalDate toLocalDateExclusive = LocalDate.ofInstant(toDateTimeExclusive.toInstant(), userZoneId);
        final Instant from = Instant.from(fromDateTime);
        final Instant toExclusive = Instant.from(toDateTimeExclusive);

        final Map<LocalDate, List<TimeEntry>> timeEntriesByDate = timeEntryRepository.findAllByOwnerAndStartGreaterThanEqualAndStartLessThanOrderByStartDesc(userId.value(), from, toExclusive)
            .stream()
            .map(timeEntryEntity -> toTimeEntry(timeEntryEntity, user))
            .collect(groupingBy(entry -> LocalDate.ofInstant(entry.start().toInstant(), userZoneId)));

        final WorkingTimeCalendar workingTimeCalendar = workingTimeCalendarService
            .getWorkingTimeCalender(fromLocalDate, toLocalDateExclusive, userLocalId);

        final List<TimeEntryDay> daysOfWeek = createTimeEntryDays(fromLocalDate, toLocalDateExclusive, timeEntriesByDate, workingTimeCalendar);

        final PlannedWorkingHours weekPlannedHours = workingTimeCalendar
            .plannedWorkingHours(fromLocalDate, toLocalDateExclusive);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(fromLocalDate, weekPlannedHours, daysOfWeek);
        final long totalTimeEntries = timeEntryRepository.countAllByOwner(userId.value());

        return new TimeEntryWeekPage(timeEntryWeek, totalTimeEntries);
    }

    @Override
    public TimeEntry createTimeEntry(UserLocalId userLocalId, String comment, ZonedDateTime start, ZonedDateTime end, boolean isBreak) {

        final User user = findUser(userLocalId);

        final TimeEntryEntity entity = new TimeEntryEntity();
        entity.setOwner(user.userIdComposite().id().value());
        entity.setComment(requireNonNullElse(comment, "").strip());
        entity.setStart(start.toInstant());
        entity.setStartZoneId(start.getZone().getId());
        entity.setEnd(end.toInstant());
        entity.setEndZoneId(end.getZone().getId());
        entity.setBreak(isBreak);

        final TimeEntry saved = save(entity, user);

        LOG.info("Created timeEntry {} of user {}. Publish TimeEntryCreated application event.", saved.id(), saved.userIdComposite().localId());
        applicationEventPublisher.publishEvent(new TimeEntryCreatedEvent(
            saved.id(),
            saved.userIdComposite(),
            timeEntryLockService.isLocked(saved.start()),
            saved.start().toLocalDate(),
            saved.workDuration()
        ));

        return saved;
    }

    @Override
    public TimeEntry updateTimeEntry(TimeEntryId id, String comment, @Nullable ZonedDateTime start, @Nullable ZonedDateTime end,
                                     @Nullable Duration duration, boolean isBreak) throws TimeEntryUpdateNotPlausibleException {

        final TimeEntryEntity entity = timeEntryRepository.findById(id.value())
            .orElseThrow(() -> new IllegalStateException("could not find existing timeEntry id=%s".formatted(id)));

        final TimeEntry previousTimeEntry = toTimeEntry(entity);

        updateEntityTimeSpan(entity, start, end, duration);

        entity.setComment(requireNonNullElse(comment, "").strip());
        entity.setBreak(isBreak);

        final TimeEntry saved = save(entity);

        LOG.info("Updated timeEntry {} of user {}. Publish TimeEntryUpdated application event.", saved.id(), saved.userIdComposite().localId());
        final LocalDate previousDate = previousTimeEntry.start().toLocalDate();
        final LocalDate currentDate = saved.start().toLocalDate();

        applicationEventPublisher.publishEvent(new TimeEntryUpdatedEvent(
            saved.id(),
            saved.userIdComposite(),
            new UpdatedValueCandidate<>(timeEntryLockService.isLocked(previousDate), timeEntryLockService.isLocked(currentDate)),
            new UpdatedValueCandidate<>(previousDate, currentDate),
            new UpdatedValueCandidate<>(previousTimeEntry.workDuration(), saved.workDuration())
        ));


        return saved;
    }

    @Override
    public void deleteTimeEntry(TimeEntryId id) {
        findTimeEntry(id).ifPresentOrElse(
            timeEntry -> {
                timeEntryRepository.deleteById(timeEntry.id().value());
                LOG.info("Deleted timeEntry {}. Publish TimeEntryDeleted application event.", id);
                final boolean locked = timeEntryLockService.isLocked(timeEntry.start());
                final WorkDuration workDuration = timeEntry.workDuration();
                final LocalDate date = timeEntry.start().toLocalDate();
                applicationEventPublisher.publishEvent(new TimeEntryDeletedEvent(id, timeEntry.userIdComposite(), locked, date, workDuration));
            },
            () -> LOG.info("Could not delete timeEntry id={} since it does not exist.", id)
        );
    }

    private List<TimeEntryDay> createTimeEntryDays(LocalDate from, LocalDate toExclusive,
                                                   Map<LocalDate, List<TimeEntry>> timeEntriesByDate,
                                                   WorkingTimeCalendar workingTimeCalendar) {

        final ZoneId userZoneId = userSettingsProvider.zoneId();
        final Optional<LocalDate> minValidTimeEntryDate = timeEntryLockService.getMinValidTimeEntryDate(userZoneId);

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
            // TODO call workingDurationCalculationService to calculate workDuration instead of doing it in the TimeEntryDay.
            //      the workingDurationCalculationService then decides wich algorithm to use (e.g. subtract overlapping entries or not)
            timeEntryDays.add(new TimeEntryDay(locked, date, plannedWorkingHours, shouldWorkingHours, timeEntries, absences));

            date = date.minusDays(1);
        }

        return timeEntryDays;
    }

    private static boolean isDateLocked(LocalDate date, Optional<LocalDate> minValidTimeEntryDate) {
        return minValidTimeEntryDate.map(date::isBefore).orElse(false);
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

    private TimeEntry save(TimeEntryEntity entity) {
        entity.setUpdatedAt(Instant.now(clock));
        return toTimeEntry(timeEntryRepository.save(entity));
    }

    private TimeEntry save(TimeEntryEntity entity, User timeEntryOwner) {
        entity.setUpdatedAt(Instant.now(clock));
        return toTimeEntry(timeEntryRepository.save(entity), timeEntryOwner);
    }

    private User findUser(UserId userId) {
        return userManagementService.findUserById(userId)
            .orElseThrow(() -> new IllegalStateException("expected user id=%s to exist but got nothing.".formatted(userId)));
    }

    private User findUser(UserLocalId userLocalId) {
        return userManagementService.findUserByLocalId(userLocalId)
            .orElseThrow(() -> new IllegalStateException("expected user localId=%s to exist but got nothing.".formatted(userLocalId)));
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
            LOG.info("Cannot map TimeEntryEntity {} because user={} does not exist anymore. Ignoring it.", timeEntryEntity.id, userId);
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

    private Instant toInstant(LocalDate localDate) {
        // this must actually be the invoker point of view.
        // this does not necessarily have to be the logged-in user!
        return localDate.atStartOfDay().atZone(userSettingsProvider.zoneId()).toInstant();
    }

    private <T> boolean hasBeenModified(Supplier<T> a, Supplier<T> b) {
        return !a.get().equals(b.get());
    }
}
