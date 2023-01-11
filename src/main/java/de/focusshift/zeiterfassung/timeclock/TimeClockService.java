package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
class TimeClockService {

    private final TimeClockRepository timeClockRepository;
    private final TimeEntryService timeEntryService;
    private final UserSettingsProvider userSettingsProvider;

    TimeClockService(TimeClockRepository timeClockRepository, TimeEntryService timeEntryService, UserSettingsProvider userSettingsProvider) {
        this.timeClockRepository = timeClockRepository;
        this.timeEntryService = timeEntryService;
        this.userSettingsProvider = userSettingsProvider;
    }

    Optional<TimeClock> getCurrentTimeClock(UserId userId) {
        return timeClockRepository.findByOwnerAndStoppedAtIsNull(userId.value()).map(TimeClockService::toTimeClock);
    }

    void startTimeClock(UserId userId) {
        final ZonedDateTime now = ZonedDateTime.now(userSettingsProvider.zoneId());
        final TimeClock timeClock = new TimeClock(userId, now);

        timeClockRepository.save(toEntity(timeClock));
    }

    TimeClock updateTimeClock(UserId userId, TimeClockUpdate timeClockUpdate) throws TimeClockNotStartedException {

        final TimeClock timeClock = getCurrentTimeClock(userId)
            .map(existingTimeClock -> prepareTimeClockUpdate(existingTimeClock, timeClockUpdate))
            .orElseThrow(() -> new TimeClockNotStartedException(userId));

        final TimeClockEntity timeClockEntity = toEntity(timeClock);

        return toTimeClock(timeClockRepository.save(timeClockEntity));
    }

    void stopTimeClock(UserId userId) {
        timeClockRepository.findByOwnerAndStoppedAtIsNull(userId.value())
            .map(entity -> timeClockEntityWithStoppedAt(entity, ZonedDateTime.now(userSettingsProvider.zoneId())))
            .map(timeClockRepository::save)
            .map(TimeClockService::toTimeClock)
            .map(TimeClockService::timeClockToTimeEntry)
            .ifPresent(timeEntryService::saveTimeEntry);
    }

    private static TimeClockEntity toEntity(TimeClock timeClock) {
        final Long id = timeClock.id();
        final String userId = timeClock.userId().value();
        final ZonedDateTime startedAt = timeClock.startedAt();
        final ZonedDateTime stoppedAt = timeClock.stoppedAt().orElse(null);
        final Instant stoppedAtInstant = stoppedAt == null ? null : stoppedAt.toInstant();
        final ZoneId stoppedAtZoneId = stoppedAt == null ? null : stoppedAt.getZone();

        return new TimeClockEntity(id, userId, startedAt.toInstant(), startedAt.getZone(), stoppedAtInstant, stoppedAtZoneId, timeClock.comment());
    }

    private static TimeClock toTimeClock(TimeClockEntity timeClockEntity) {
        final Long id = timeClockEntity.getId();
        final UserId userId = new UserId(timeClockEntity.getOwner());
        final ZonedDateTime startedAt = ZonedDateTime.ofInstant(timeClockEntity.getStartedAt(), ZoneId.of(timeClockEntity.getStartedAtZoneId()));
        final ZonedDateTime stoppedAt = timeClockEntity.getStoppedAt() == null ? null : ZonedDateTime.ofInstant(timeClockEntity.getStoppedAt(), ZoneId.of(timeClockEntity.getStoppedAtZoneId()));

        return new TimeClock(id, userId, startedAt, timeClockEntity.getComment(), Optional.ofNullable(stoppedAt));
    }

    private static TimeEntry timeClockToTimeEntry(TimeClock timeClock) {

        final UserId userId = timeClock.userId();
        final ZonedDateTime startedAt = timeClock.startedAt();
        final ZonedDateTime stoppedAt = timeClock.stoppedAt()
            .orElseThrow(() -> new IllegalArgumentException("expected timeClock with stoppedAt field."));

        return new TimeEntry(null, userId, "", startedAt, stoppedAt);
    }

    private static TimeClock prepareTimeClockUpdate(TimeClock existingTimeClock, TimeClockUpdate timeClockUpdate) {
        return TimeClock.builder(existingTimeClock)
            .startedAt(timeClockUpdate.startedAt())
            .comment(timeClockUpdate.comment())
            .build();
    }

    private static TimeClockEntity timeClockEntityWithStoppedAt(TimeClockEntity entity, ZonedDateTime stoppedAt) {
        return new TimeClockEntity(entity.getId(), entity.getOwner(), entity.getStartedAt(), ZoneId.of(entity.getStartedAtZoneId()), stoppedAt.toInstant(), stoppedAt.getZone());
    }
}
