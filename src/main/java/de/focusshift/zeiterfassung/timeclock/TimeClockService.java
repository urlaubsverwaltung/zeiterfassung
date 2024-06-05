package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TimeClockService {

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
            .ifPresent(timeClock -> {

                final ZonedDateTime start = timeClock.startedAt();
                final ZonedDateTime end = timeClock.stoppedAt()
                    .orElseThrow(() -> new IllegalStateException("expected stoppedAt to contain a value."));

                timeEntryService.createTimeEntry(userId, timeClock.comment(), start, end, timeClock.isBreak());
            });
    }

    public List<TimeClock> findAllTimeClocks(UserId userId) {
        return timeClockRepository.findAllByOwnerOrderByIdAsc(userId.value()).stream()
            .map(TimeClockService::toTimeClock)
            .toList();
    }

    private static TimeClockEntity toEntity(TimeClock timeClock) {
        return TimeClockEntity.builder()
            .id(timeClock.id())
            .owner(timeClock.userId().value())
            .startedAt(timeClock.startedAt().toInstant())
            .startedAtZoneId(timeClock.startedAt().getZone())
            .stoppedAt(timeClock.stoppedAt().map(ZonedDateTime::toInstant).orElse(null))
            .stoppedAtZoneId(timeClock.stoppedAt().map(ZonedDateTime::getZone).orElse(null))
            .comment(timeClock.comment())
            .isBreak(timeClock.isBreak())
            .build();
    }

    private static TimeClock toTimeClock(TimeClockEntity timeClockEntity) {
        final Long id = timeClockEntity.getId();
        final UserId userId = new UserId(timeClockEntity.getOwner());
        final ZonedDateTime startedAt = ZonedDateTime.ofInstant(timeClockEntity.getStartedAt(), ZoneId.of(timeClockEntity.getStartedAtZoneId()));
        final ZonedDateTime stoppedAt = timeClockEntity.getStoppedAt() == null ? null : ZonedDateTime.ofInstant(timeClockEntity.getStoppedAt(), ZoneId.of(timeClockEntity.getStoppedAtZoneId()));

        return new TimeClock(id, userId, startedAt, timeClockEntity.getComment(), timeClockEntity.isBreak(), Optional.ofNullable(stoppedAt));
    }

    private static TimeEntry timeClockToTimeEntry(TimeClock timeClock) {

        final UserId userId = timeClock.userId();
        final ZonedDateTime startedAt = timeClock.startedAt();
        final ZonedDateTime stoppedAt = timeClock.stoppedAt()
            .orElseThrow(() -> new IllegalArgumentException("expected timeClock with stoppedAt field."));

        return new TimeEntry(null, userId, timeClock.comment(), startedAt, stoppedAt, timeClock.isBreak());
    }

    private static TimeClock prepareTimeClockUpdate(TimeClock existingTimeClock, TimeClockUpdate timeClockUpdate) {
        return TimeClock.builder(existingTimeClock)
            .startedAt(timeClockUpdate.startedAt())
            .comment(timeClockUpdate.comment())
            .isBreak(timeClockUpdate.isBreak())
            .build();
    }

    private static TimeClockEntity timeClockEntityWithStoppedAt(TimeClockEntity entity, ZonedDateTime stoppedAt) {
        return TimeClockEntity.builder(entity)
            .stoppedAt(stoppedAt.toInstant())
            .stoppedAtZoneId(stoppedAt.getZone())
            .build();
    }
}
