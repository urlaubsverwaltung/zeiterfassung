package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.timeentry.events.DayLockedEvent;
import org.slf4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.util.stream.Stream.iterate;
import static java.util.stream.StreamSupport.stream;
import static org.slf4j.LoggerFactory.getLogger;

@Service
class SettingsService implements FederalStateSettingsService, WorkingTimeSettingsService,
    LockTimeEntriesSettingsService, SubtractBreakFromTimeEntrySettingsService, OooCalendarSettingsService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final FederalStateSettingsRepository federalStateSettingsRepository;
    private final WorkingTimeSettingsRepository workingTimeSettingsRepository;
    private final LockTimeEntriesSettingsRepository lockTimeEntriesSettingsRepository;
    private final SubtractBreakFromTimeEntrySettingsRepository subtractBreakFromTimeEntrySettingsRepository;
    private final OooCalendarSettingsRepository oooCalendarSettingsRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    SettingsService(
        FederalStateSettingsRepository federalStateSettingsRepository,
        WorkingTimeSettingsRepository workingTimeSettingsRepository,
        LockTimeEntriesSettingsRepository lockTimeEntriesSettingsRepository,
        SubtractBreakFromTimeEntrySettingsRepository subtractBreakFromTimeEntrySettingsRepository,
        OooCalendarSettingsRepository oooCalendarSettingsRepository,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.federalStateSettingsRepository = federalStateSettingsRepository;
        this.workingTimeSettingsRepository = workingTimeSettingsRepository;
        this.lockTimeEntriesSettingsRepository = lockTimeEntriesSettingsRepository;
        this.subtractBreakFromTimeEntrySettingsRepository = subtractBreakFromTimeEntrySettingsRepository;
        this.oooCalendarSettingsRepository = oooCalendarSettingsRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public FederalStateSettings getFederalStateSettings() {
        return findFirstEntity(federalStateSettingsRepository.findAll())
            .map(SettingsService::toFederalStateSettings)
            .orElse(FederalStateSettings.DEFAULT);
    }

    FederalStateSettings updateFederalStateSettings(FederalState federalState, boolean worksOnPublicHoliday) {
        final FederalStateSettingsEntity entity = findFirstEntity(federalStateSettingsRepository.findAll())
            .orElseGet(FederalStateSettingsEntity::new);
        entity.setFederalState(federalState);
        entity.setWorksOnPublicHoliday(worksOnPublicHoliday);
        return toFederalStateSettings(federalStateSettingsRepository.save(entity));
    }

    @Override
    public WorkingTimeSettings getWorkingTimeSettings() {
        return findFirstEntity(workingTimeSettingsRepository.findAll())
            .map(SettingsService::toWorkingTimeSettings)
            .orElse(WorkingTimeSettings.DEFAULT);
    }

    WorkingTimeSettings updateWorkingTimeSettings(EnumMap<DayOfWeek, Duration> workdays) {
        final WorkingTimeSettingsEntity entity = findFirstEntity(workingTimeSettingsRepository.findAll())
            .orElseGet(WorkingTimeSettingsEntity::new);
        entity.setMonday(durationToString(workdays.get(MONDAY)));
        entity.setTuesday(durationToString(workdays.get(TUESDAY)));
        entity.setWednesday(durationToString(workdays.get(WEDNESDAY)));
        entity.setThursday(durationToString(workdays.get(THURSDAY)));
        entity.setFriday(durationToString(workdays.get(FRIDAY)));
        entity.setSaturday(durationToString(workdays.get(SATURDAY)));
        entity.setSunday(durationToString(workdays.get(SUNDAY)));
        return toWorkingTimeSettings(workingTimeSettingsRepository.save(entity));
    }

    @Override
    public LockTimeEntriesSettings getLockTimeEntriesSettings() {
        return findFirstEntity(lockTimeEntriesSettingsRepository.findAll())
            .map(SettingsService::toLockTimeEntriesSettings)
            .orElse(LockTimeEntriesSettings.DEFAULT);
    }

    LockTimeEntriesSettings updateLockTimeEntriesSettings(boolean lockingIsActive, int lockTimeEntriesDaysInPast) {
        final LockTimeEntriesSettingsEntity entity = findFirstEntity(lockTimeEntriesSettingsRepository.findAll())
            .orElseGet(LockTimeEntriesSettingsEntity::new);
        final int previousLockTimeEntriesDaysInPast = entity.getLockTimeEntriesDaysInPast();
        entity.setLockingIsActive(lockingIsActive);
        entity.setLockTimeEntriesDaysInPast(lockTimeEntriesDaysInPast);
        final LockTimeEntriesSettingsEntity saved = lockTimeEntriesSettingsRepository.save(entity);
        if (saved.isLockingIsActive()) {
            LOG.info("LockTimeEntriesSettings updated: locking is active. Looking for updated DayLocked events now (previousLockTimeEntriesDaysInPast={} lockTimeEntriesDaysInPast={})", previousLockTimeEntriesDaysInPast, lockTimeEntriesDaysInPast);
            publishedDayLockedEvents(previousLockTimeEntriesDaysInPast, lockTimeEntriesDaysInPast);
        } else {
            LOG.info("LockTimeEntriesSettings updated: locking is disabled.");
        }
        return toLockTimeEntriesSettings(saved);
    }

    @Override
    public Optional<SubtractBreakFromTimeEntrySettings> getSubtractBreakFromTimeEntrySettings() {
        return findFirstEntity(subtractBreakFromTimeEntrySettingsRepository.findAll())
            .map(SettingsService::toSubtractBreakFromTimeEntrySettings);
    }

    SubtractBreakFromTimeEntrySettings updateSubtractBreakFromTimeEntrySettings(boolean featureActive, Instant featureActiveTimestamp) {
        final SubtractBreakFromTimeEntrySettingsEntity entity = findFirstEntity(subtractBreakFromTimeEntrySettingsRepository.findAll())
            .orElseGet(SubtractBreakFromTimeEntrySettingsEntity::new);
        entity.setSubtractBreakFromTimeEntryIsActive(featureActive);
        entity.setSubtractBreakFromTimeEntryEnabledTimestamp(featureActiveTimestamp);
        return toSubtractBreakFromTimeEntrySettings(subtractBreakFromTimeEntrySettingsRepository.save(entity));
    }

    @Override
    public OooCalendarSettings getOooCalendarSettings() {
        return findFirstEntity(oooCalendarSettingsRepository.findAll())
            .map(e -> new OooCalendarSettings(e.getCalendarUrl()))
            .orElse(OooCalendarSettings.DEFAULT);
    }

    OooCalendarSettings updateOooCalendarSettings(String calendarUrl) {
        final OooCalendarSettingsEntity entity = findFirstEntity(oooCalendarSettingsRepository.findAll())
            .orElseGet(OooCalendarSettingsEntity::new);
        entity.setCalendarUrl(calendarUrl == null || calendarUrl.isBlank() ? null : calendarUrl.strip());
        return new OooCalendarSettings(oooCalendarSettingsRepository.save(entity).getCalendarUrl());
    }

    private void publishedDayLockedEvents(final int previousLockTimeEntriesDaysInPast, final int actualLockTimeEntriesDaysInPast) {
        final ZoneId zoneId = ZoneId.of("Europe/Berlin");
        final LocalDate today = LocalDate.now(zoneId);
        final LocalDate oldLockTimeEntryDate = today.minusDays(previousLockTimeEntriesDaysInPast).minusDays(1);
        final LocalDate actualLockTimeEntryDate = today.minusDays(actualLockTimeEntriesDaysInPast).minusDays(1);
        iterate(oldLockTimeEntryDate, date -> !date.isAfter(actualLockTimeEntryDate), date -> date.plusDays(1))
            .forEach(lockedDate -> applicationEventPublisher.publishEvent(new DayLockedEvent(lockedDate, zoneId)));
    }

    private static <T> Optional<T> findFirstEntity(Iterable<T> entities) {
        // `findFirst` is sufficient as there exists only one settings entity per tenant;
        // tenantId is handled transparently, and we only have the public API of `findAll`.
        return stream(entities.spliterator(), false).findFirst();
    }

    private static String durationToString(Duration duration) {
        return duration == null ? Duration.ZERO.toString() : duration.toString();
    }

    private static FederalStateSettings toFederalStateSettings(FederalStateSettingsEntity e) {
        return new FederalStateSettings(e.getFederalState(), e.isWorksOnPublicHoliday());
    }

    private static WorkingTimeSettings toWorkingTimeSettings(WorkingTimeSettingsEntity e) {
        final EnumMap<DayOfWeek, Duration> workdays = new EnumMap<>(DayOfWeek.class);
        workdays.put(MONDAY,    Duration.parse(e.getMonday()));
        workdays.put(TUESDAY,   Duration.parse(e.getTuesday()));
        workdays.put(WEDNESDAY, Duration.parse(e.getWednesday()));
        workdays.put(THURSDAY,  Duration.parse(e.getThursday()));
        workdays.put(FRIDAY,    Duration.parse(e.getFriday()));
        workdays.put(SATURDAY,  Duration.parse(e.getSaturday()));
        workdays.put(SUNDAY,    Duration.parse(e.getSunday()));
        return new WorkingTimeSettings(workdays);
    }

    private static LockTimeEntriesSettings toLockTimeEntriesSettings(LockTimeEntriesSettingsEntity e) {
        return new LockTimeEntriesSettings(e.isLockingIsActive(), e.getLockTimeEntriesDaysInPast());
    }

    private static SubtractBreakFromTimeEntrySettings toSubtractBreakFromTimeEntrySettings(SubtractBreakFromTimeEntrySettingsEntity e) {
        return new SubtractBreakFromTimeEntrySettings(
            e.isSubtractBreakFromTimeEntryIsActive(),
            Optional.ofNullable(e.getSubtractBreakFromTimeEntryEnabledTimestamp())
        );
    }
}
