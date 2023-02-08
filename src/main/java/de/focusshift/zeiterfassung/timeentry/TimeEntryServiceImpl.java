package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Comparator.comparing;

@Service
class TimeEntryServiceImpl implements TimeEntryService {

    public static final BigDecimal ONE_MINUTE_IN_SECONDS = BigDecimal.valueOf(60);

    private final TimeEntryRepository timeEntryRepository;
    private final UserManagementService userManagementService;
    private final UserDateService userDateService;
    private final Clock clock;

    @Autowired
    TimeEntryServiceImpl(TimeEntryRepository timeEntryRepository, UserManagementService userManagementService,
                         UserDateService userDateService, Clock clock) {

        this.timeEntryRepository = timeEntryRepository;
        this.userManagementService = userManagementService;
        this.userDateService = userDateService;
        this.clock = clock;
    }

    @Override
    public List<TimeEntry> getEntries(LocalDate from, LocalDate toExclusive, UserId userId) {

        final Instant fromInstant = toInstant(from);
        final Instant toInstant = toInstant(toExclusive);

        final List<TimeEntryEntity> result = timeEntryRepository
            .findAllByOwnerAndTouchingPeriod(userId.value(), fromInstant, toInstant);

        return result
            .stream()
            .map(TimeEntryServiceImpl::toTimeEntry)
            .toList();
    }

    @Override
    public List<TimeEntry> getEntriesForAllUsers(LocalDate from, LocalDate toExclusive) {

        final Instant fromInstant = toInstant(from);
        final Instant toInstant = toInstant(toExclusive);

        return timeEntryRepository.findAllByTouchingPeriod(fromInstant, toInstant)
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
            .findAllByOwnerIsInAndTouchingPeriod(userIdValues, fromInstant, toInstant);

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

        final List<TimeEntry> timeEntries = timeEntryRepository.findAllByOwnerAndTouchingPeriod(userId.value(), from, to)
            .stream()
            .sorted(comparing(TimeEntryEntity::getStart).thenComparing(TimeEntryEntity::getUpdatedAt).reversed())
            .map(TimeEntryServiceImpl::toTimeEntry)
            .toList();

        List<TimeEntryDay> daysOfWeek = timeEntries.stream()
            .collect(Collectors.groupingBy(timeEntry -> timeEntry.start().toLocalDate()))
            .entrySet().stream()
            .map(e -> new TimeEntryDay(e.getKey(), e.getValue()))
            .sorted(comparing(TimeEntryDay::date).reversed())
            .toList();

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(fromDateTime.toLocalDate(), daysOfWeek);
        final long totalTimeEntries = timeEntryRepository.countAllByOwner(userId.value());

        return new TimeEntryWeekPage(timeEntryWeek, totalTimeEntries);
    }

    @Override
    public TimeEntry saveTimeEntry(TimeEntry timeEntry) {
        final TimeEntryEntity savedEntity = timeEntryRepository.save(toEntity(timeEntry, Instant.now(clock)));
        return toTimeEntry(savedEntity);
    }

    @Override
    public void deleteTimeEntry(long timeEntryId) {
        timeEntryRepository.deleteById(timeEntryId);
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

        return new TimeEntry(entity.getId(), userId, entity.getComment(), startDateTime, endDateTime, entity.isBreak());
    }

    private TimeEntryEntity toEntity(TimeEntry timeEntry, Instant updatedAt) {
        final Instant start = timeEntry.start().toInstant();
        final ZoneId startZoneId = timeEntry.start().getZone();
        final Instant end = timeEntry.end().toInstant();
        final ZoneId endZoneId = timeEntry.end().getZone();

        return new TimeEntryEntity(timeEntry.id(), timeEntry.userId().value(), timeEntry.comment(), start, startZoneId, end, endZoneId, updatedAt, timeEntry.isBreak());
    }

    private static Instant toInstant(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
