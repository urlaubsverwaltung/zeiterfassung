package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.usermanagement.WorkingTimeService;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;

@Service
class TimeEntryServiceImpl implements TimeEntryService {

    public static final BigDecimal ONE_MINUTE_IN_SECONDS = BigDecimal.valueOf(60);

    private final TimeEntryRepository timeEntryRepository;
    private final UserManagementService userManagementService;
    private final WorkingTimeService workingTimeService;
    private final UserDateService userDateService;
    private final Clock clock;

    @Autowired
    TimeEntryServiceImpl(TimeEntryRepository timeEntryRepository, UserManagementService userManagementService,
                         WorkingTimeService workingTimeService, UserDateService userDateService, Clock clock) {

        this.timeEntryRepository = timeEntryRepository;
        this.userManagementService = userManagementService;
        this.workingTimeService = workingTimeService;
        this.userDateService = userDateService;
        this.clock = clock;
    }

    @Override
    public Optional<TimeEntry> findTimeEntry(long id) {
        return timeEntryRepository.findById(id).map(TimeEntryServiceImpl::toTimeEntry);
    }

    @Override
    public List<TimeEntry> getEntries(LocalDate from, LocalDate toExclusive, UserId userId) {

        final Instant fromInstant = toInstant(from);
        final Instant toInstant = toInstant(toExclusive);

        final List<TimeEntryEntity> result = timeEntryRepository
            .findAllByOwnerAndStartGreaterThanEqualAndStartLessThan(userId.value(), fromInstant, toInstant);

        return result
            .stream()
            .map(TimeEntryServiceImpl::toTimeEntry)
            .sorted(comparing(TimeEntry::start).reversed())
            .toList();
    }

    @Override
    public List<TimeEntry> getEntriesForAllUsers(LocalDate from, LocalDate toExclusive) {

        final Instant fromInstant = toInstant(from);
        final Instant toInstant = toInstant(toExclusive);

        return timeEntryRepository.findAllByStartGreaterThanEqualAndStartLessThan(fromInstant, toInstant)
            .stream()
            .map(TimeEntryServiceImpl::toTimeEntry)
            .toList();
    }

    @Override
    public List<TimeEntry> getEntriesByUserLocalIds(LocalDate from, LocalDate toExclusive, List<UserLocalId> userLocalIds) {

        final Instant fromInstant = toInstant(from);
        final Instant toInstant = toInstant(toExclusive);

        final List<String> userIdValues = userManagementService.findAllUsersByLocalIds(userLocalIds)
            .stream()
            .map(User::id)
            .map(UserId::value)
            .toList();

        final List<TimeEntryEntity> result = timeEntryRepository
            .findAllByOwnerIsInAndStartGreaterThanEqualAndStartLessThan(userIdValues, fromInstant, toInstant);

        return result
            .stream()
            .map(TimeEntryServiceImpl::toTimeEntry)
            .toList();
    }

    @Override
    public TimeEntryWeekPage getEntryWeekPage(UserId userId, int year, int weekOfYear) {

        final ZonedDateTime fromDateTime = userDateService.firstDayOfWeek(Year.of(year), weekOfYear).atStartOfDay(ZoneId.systemDefault());
        final Instant from = Instant.from(fromDateTime);
        final Instant to = Instant.from(fromDateTime.plusWeeks(1));

        final List<TimeEntry> timeEntries = timeEntryRepository.findAllByOwnerAndStartGreaterThanEqualAndStartLessThan(userId.value(), from, to)
            .stream()
            .sorted(comparing(TimeEntryEntity::getStart).thenComparing(TimeEntryEntity::getUpdatedAt).reversed())
            .map(TimeEntryServiceImpl::toTimeEntry)
            .toList();

        // TODO refactor getEntryWeekPage to accept UserLocalId
        final User user = userManagementService.findUserById(userId).orElseThrow(() -> new IllegalStateException("expected user=%s to exist.".formatted(userId)));
        final UserLocalId userLocalId = user.localId();
        final Map<LocalDate, PlannedWorkingHours> plannedByDate = workingTimeService.getWorkingHoursByUserAndYearWeek(userLocalId, Year.of(year), weekOfYear);

        final PlannedWorkingHours weekPlannedHours = plannedByDate.values()
            .stream()
            .reduce(PlannedWorkingHours.ZERO, PlannedWorkingHours::plus);

        final List<TimeEntryDay> daysOfWeek = timeEntries.stream()
            .collect(groupingBy(timeEntry -> timeEntry.start().toLocalDate()))
            .entrySet().stream()
            .map(e -> new TimeEntryDay(e.getKey(), plannedByDate.get(e.getKey()), e.getValue()))
            .sorted(comparing(TimeEntryDay::date).reversed())
            .toList();

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(fromDateTime.toLocalDate(), weekPlannedHours, daysOfWeek);
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
    public TimeEntry updateTimeEntry(TimeEntryId id, String comment, ZonedDateTime start, ZonedDateTime end,
                                     Duration duration, boolean isBreak) throws TimeEntryUpdateException {

        final TimeEntryEntity entity = timeEntryRepository.findById(id.value())
            .orElseThrow(() -> new IllegalStateException("could not find existing timeEntry id=%s".formatted(id)));

        final TimeEntry existingTimeEntry = toTimeEntry(entity);
        final boolean startChanged = notEquals(existingTimeEntry.start(), start);
        final boolean endChanged = notEquals(existingTimeEntry.end(), end);
        final boolean durationChanged = !existingTimeEntry.duration().value().equals(duration);
        final boolean plausibleUpdate = delta(start, end).equals(duration);

        if (startChanged && endChanged && durationChanged && !plausibleUpdate) {
            throw new TimeEntryUpdateException("cannot update time-entry when start, end and duration should be changed. pick two of them.");
        }

        if (plausibleUpdate || startChanged) {
            entity.setStart(start.toInstant());
            entity.setStartZoneId(start.getZone().getId());
            if (durationChanged) {
                final ZonedDateTime newEnd = start.plusMinutes(duration.toMinutes());
                entity.setEnd(newEnd.toInstant());
                entity.setEndZoneId(newEnd.getZone().getId());
            }
        }

        if (plausibleUpdate || endChanged) {
            entity.setEnd(end.toInstant());
            entity.setEndZoneId(end.getZone().getId());
            if (durationChanged) {
                final ZonedDateTime newStart = end.minusMinutes(duration.toMinutes());
                entity.setStart(newStart.toInstant());
                entity.setStartZoneId(newStart.getZone().getId());
            }
        }

        if (durationChanged && !startChanged && !endChanged) {
            final ZonedDateTime newEnd = start.plusMinutes(duration.toMinutes());
            entity.setEnd(newEnd.toInstant());
            entity.setEndZoneId(newEnd.getZone().getId());
        }

        entity.setComment(comment.strip());
        entity.setBreak(isBreak);

        return save(entity);
    }

    private Duration delta(ZonedDateTime first, ZonedDateTime second) {
        return Duration.ofMinutes(MINUTES.between(first, second));
    }

    private boolean notEquals(ZonedDateTime one, ZonedDateTime two) {
        return !one.toInstant().atZone(ZoneOffset.UTC).equals(two.toInstant().atZone(ZoneOffset.UTC));
    }

    @Override
    public void deleteTimeEntry(long timeEntryId) {
        timeEntryRepository.deleteById(timeEntryId);
    }

    private TimeEntry save(TimeEntryEntity entity) {
        entity.setUpdatedAt(Instant.now(clock));
        return toTimeEntry(timeEntryRepository.save(entity));
    }

    private static TimeEntry toTimeEntry(TimeEntryEntity entity) {
        final Instant actualStart = entity.getStart();
        final Instant actualEnd = entity.getEnd();

        final BigDecimal minutesDelta = BigDecimal.valueOf(actualStart.until(actualEnd, SECONDS))
            .divide(ONE_MINUTE_IN_SECONDS, RoundingMode.UP);

        final Instant start = actualStart.truncatedTo(MINUTES);
        final Instant end = start.plus(minutesDelta.longValue(), MINUTES);

        final ZonedDateTime startDateTime = ZonedDateTime.ofInstant(start, ZoneId.of(entity.getStartZoneId()));
        final ZonedDateTime endDateTime = ZonedDateTime.ofInstant(end, ZoneId.of(entity.getEndZoneId()));

        final UserId userId = new UserId(entity.getOwner());

        return new TimeEntry(new TimeEntryId(entity.getId()), userId, entity.getComment(), startDateTime, endDateTime, entity.isBreak());
    }

    private static Instant toInstant(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
