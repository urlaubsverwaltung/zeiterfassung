package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.timeentry.events.DayLockedEvent;
import org.slf4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Stream.iterate;
import static java.util.stream.StreamSupport.stream;
import static org.slf4j.LoggerFactory.getLogger;

@Service
class SettingsService implements FederalStateSettingsService, LockTimeEntriesSettingsService, SubtractBreakFromTimeEntrySettingsService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final FederalStateSettingsRepository federalStateSettingsRepository;
    private final LockTimeEntriesSettingsRepository lockTimeEntriesSettingsRepository;
    private final SubtractBreakFromTimeEntrySettingsRepository subtractBreakFromTimeEntrySettingsRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    SettingsService(
        FederalStateSettingsRepository federalStateSettingsRepository,
        LockTimeEntriesSettingsRepository lockTimeEntriesSettingsRepository,
        SubtractBreakFromTimeEntrySettingsRepository subtractBreakFromTimeEntrySettingsRepository,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.federalStateSettingsRepository = federalStateSettingsRepository;
        this.lockTimeEntriesSettingsRepository = lockTimeEntriesSettingsRepository;
        this.subtractBreakFromTimeEntrySettingsRepository = subtractBreakFromTimeEntrySettingsRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public FederalStateSettings getFederalStateSettings() {
        return getFederalStateEntity()
            .map(SettingsService::toFederalStateSettings)
            .orElse(FederalStateSettings.DEFAULT);
    }

    @Override
    public LockTimeEntriesSettings getLockTimeEntriesSettings() {
        return getLockTimeEntriesSettingsEntity()
            .map(SettingsService::toLockTimeEntriesSettings)
            .orElse(LockTimeEntriesSettings.DEFAULT);
    }

    @Override
    public Optional<SubtractBreakFromTimeEntrySettings> getSubtractBreakFromTimeEntrySettings() {
        return getSubtractBreakFromTimeEntrySettingsEntity()
            .map(SettingsService::toSubtractBreakFromTimeEntrySettings);
    }

    /**
     * Updates {@link FederalStateSettings}.
     *
     * @param federalState         {@link FederalState} used for every person if not overridden
     * @param worksOnPublicHoliday whether persons have to work or not on public holidays
     * @return the updated {@link FederalStateSettings}
     */
    FederalStateSettings updateFederalStateSettings(FederalState federalState, boolean worksOnPublicHoliday) {

        final FederalStateSettingsEntity entity = getFederalStateEntity().orElseGet(FederalStateSettingsEntity::new);
        entity.setFederalState(federalState);
        entity.setWorksOnPublicHoliday(worksOnPublicHoliday);

        final FederalStateSettingsEntity saved = federalStateSettingsRepository.save(entity);

        return toFederalStateSettings(saved);
    }

    /**
     * Update {@link LockTimeEntriesSettings}.
     *
     * @param lockingIsActive           whether locking is active or not
     * @param lockTimeEntriesDaysInPast number of days time entries in past get locked, zero based (0 -> yesterday is locked)
     * @return the updated {@link LockTimeEntriesSettings}
     */
    LockTimeEntriesSettings updateLockTimeEntriesSettings(boolean lockingIsActive, int lockTimeEntriesDaysInPast) {

        final LockTimeEntriesSettingsEntity entity = getLockTimeEntriesSettingsEntity().orElseGet(LockTimeEntriesSettingsEntity::new);
        final int previousLockTimeEntriesDaysInPast = entity.getLockTimeEntriesDaysInPast();

        // set new values
        entity.setLockingIsActive(lockingIsActive);
        entity.setLockTimeEntriesDaysInPast(lockTimeEntriesDaysInPast);

        final LockTimeEntriesSettingsEntity saved = lockTimeEntriesSettingsRepository.save(entity);

        if (saved.isLockingIsActive()) {
            LOG.info("LockTimeEntriesSettings updated: locking is active. Looking for updated DayLocked events now.");
            publishedDayLockedEvents(previousLockTimeEntriesDaysInPast, lockTimeEntriesDaysInPast);
        }

        return toLockTimeEntriesSettings(saved);
    }

    /**
     * Updates {@link SubtractBreakFromTimeEntrySettings}.
     *
     * @param featureActive whether the feature is active or not
     * @param featureActiveTimestamp timestamp from which the feature is active
     */
    SubtractBreakFromTimeEntrySettings updateSubtractBreakFromTimeEntrySettings(
        boolean featureActive,
        Instant featureActiveTimestamp
    ) {

        final SubtractBreakFromTimeEntrySettingsEntity entity = getSubtractBreakFromTimeEntrySettingsEntity()
            .orElseGet(SubtractBreakFromTimeEntrySettingsEntity::new);

        entity.setSubtractBreakFromTimeEntryIsActive(featureActive);
        entity.setSubtractBreakFromTimeEntryEnabledTimestamp(featureActiveTimestamp);

        final SubtractBreakFromTimeEntrySettingsEntity saved = subtractBreakFromTimeEntrySettingsRepository.save(entity);
        return toSubtractBreakFromTimeEntrySettings(saved);
    }

    private Optional<FederalStateSettingsEntity> getFederalStateEntity() {
        // `findFirst` is sufficient as there exists only one FederalStateSettingsEntity per tenant.
        // however, the tenantId is handled transparently in the background. and we only have the public API of `findAll`.
        final Iterable<FederalStateSettingsEntity> settings = federalStateSettingsRepository.findAll();
        return stream(settings.spliterator(), false).findFirst();
    }

    private void publishedDayLockedEvents(final int previousLockTimeEntriesDaysInPast, final int actualLockTimeEntriesDaysInPast) {
        final ZoneId zoneId = ZoneId.of("Europe/Berlin");
        final LocalDate today = LocalDate.now(zoneId);
        final LocalDate oldLockTimeEntryDate = today.minusDays(previousLockTimeEntriesDaysInPast).minusDays(1);
        final LocalDate actualLockTimeEntryDate = today.minusDays(actualLockTimeEntriesDaysInPast).minusDays(1);

        iterate(oldLockTimeEntryDate, date -> !date.isAfter(actualLockTimeEntryDate), date -> date.plusDays(1))
            .forEach(lockedDate -> applicationEventPublisher.publishEvent(new DayLockedEvent(lockedDate, zoneId)));
    }

    private Optional<LockTimeEntriesSettingsEntity> getLockTimeEntriesSettingsEntity() {
        // `findFirst` is sufficient as there exists only one FederalStateSettingsEntity per tenant.
        // however, the tenantId is handled transparently in the background. and we only have the public API of `findAll`.
        final Iterable<LockTimeEntriesSettingsEntity> settings = lockTimeEntriesSettingsRepository.findAll();
        return stream(settings.spliterator(), false).findFirst();
    }

    private Optional<SubtractBreakFromTimeEntrySettingsEntity> getSubtractBreakFromTimeEntrySettingsEntity() {
        // `findFirst` is sufficient as there exists only one FederalStateSettingsEntity per tenant.
        // however, the tenantId is handled transparently in the background. and we only have the public API of `findAll`.
        final Iterable<SubtractBreakFromTimeEntrySettingsEntity> settings = subtractBreakFromTimeEntrySettingsRepository.findAll();
        return stream(settings.spliterator(), false).findFirst();
    }

    private static FederalStateSettings toFederalStateSettings(FederalStateSettingsEntity federalStateSettingsEntity) {
        return new FederalStateSettings(
            federalStateSettingsEntity.getFederalState(),
            federalStateSettingsEntity.isWorksOnPublicHoliday()
        );
    }

    private static LockTimeEntriesSettings toLockTimeEntriesSettings(LockTimeEntriesSettingsEntity lockTimeEntriesSettingsEntity) {
        return new LockTimeEntriesSettings(
            lockTimeEntriesSettingsEntity.isLockingIsActive(),
            lockTimeEntriesSettingsEntity.getLockTimeEntriesDaysInPast()
        );
    }

    private static SubtractBreakFromTimeEntrySettings toSubtractBreakFromTimeEntrySettings(SubtractBreakFromTimeEntrySettingsEntity entity) {
        return new SubtractBreakFromTimeEntrySettings(
            entity.isSubtractBreakFromTimeEntryIsActive(),
            Optional.ofNullable(entity.getSubtractBreakFromTimeEntryEnabledTimestamp())
        );
    }
}
