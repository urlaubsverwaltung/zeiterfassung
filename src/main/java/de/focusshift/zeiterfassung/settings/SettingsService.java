package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.util.stream.StreamSupport.stream;

@Service
class SettingsService implements FederalStateSettingsService, LockTimeEntriesSettingsService {

    private final FederalStateSettingsRepository federalStateSettingsRepository;
    private final LockTimeEntriesSettingsRepository lockTimeEntriesSettingsRepository;

    SettingsService(
        FederalStateSettingsRepository federalStateSettingsRepository,
        LockTimeEntriesSettingsRepository lockTimeEntriesSettingsRepository
    ) {
        this.federalStateSettingsRepository = federalStateSettingsRepository;
        this.lockTimeEntriesSettingsRepository = lockTimeEntriesSettingsRepository;
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

    @Override
    public FederalStateSettings getFederalStateSettings() {
        return getFederalStateEntity()
            .map(SettingsService::toFederalStateSettings)
            .orElse(FederalStateSettings.DEFAULT);
    }

    /**
     * Updates {@link FederalStateSettings}.
     *
     * @param federalState {@link FederalState} used for every person if not overridden
     * @param worksOnPublicHoliday whether persons have to work or not on public holidays
     *
     * @return the updated {@link FederalStateSettings}
     */
    FederalStateSettings updateFederalStateSettings(FederalState federalState, boolean worksOnPublicHoliday) {

        final FederalStateSettingsEntity entity = getFederalStateEntity().orElseGet(FederalStateSettingsEntity::new);
        entity.setFederalState(federalState);
        entity.setWorksOnPublicHoliday(worksOnPublicHoliday);

        final FederalStateSettingsEntity saved = federalStateSettingsRepository.save(entity);

        return toFederalStateSettings(saved);
    }

    private Optional<FederalStateSettingsEntity> getFederalStateEntity() {
        // `findFirst` is sufficient as there exists only one FederalStateSettingsEntity per tenant.
        // however, the tenantId is handled transparently in the background. and we only have the public API of `findAll`.
        final Iterable<FederalStateSettingsEntity> settings = federalStateSettingsRepository.findAll();
        return stream(settings.spliterator(), false).findFirst();
    }

    @Override
    public LockTimeEntriesSettings getLockTimeEntriesSettings() {
        return getLockTimeEntriesSettingsEntity()
            .map(SettingsService::toLockTimeEntriesSettings)
            .orElse(LockTimeEntriesSettings.DEFAULT);
    }

    /**
     * Update {@link LockTimeEntriesSettings}.
     *
     * @param lockingIsActive whether locking is active or not
     * @param lockTimeEntriesDaysInPast number of days time entries in past get locked
     *
     * @return the updated {@link LockTimeEntriesSettings}
     */
    LockTimeEntriesSettings updateLockTimeEntriesSettings(boolean lockingIsActive, int lockTimeEntriesDaysInPast) {

        final LockTimeEntriesSettingsEntity entity = getLockTimeEntriesSettingsEntity().orElseGet(LockTimeEntriesSettingsEntity::new);
        entity.setLockingIsActive(lockingIsActive);
        entity.setLockTimeEntriesDaysInPast(lockTimeEntriesDaysInPast);

        final LockTimeEntriesSettingsEntity saved = lockTimeEntriesSettingsRepository.save(entity);

        return toLockTimeEntriesSettings(saved);
    }

    private Optional<LockTimeEntriesSettingsEntity> getLockTimeEntriesSettingsEntity() {
        // `findFirst` is sufficient as there exists only one FederalStateSettingsEntity per tenant.
        // however, the tenantId is handled transparently in the background. and we only have the public API of `findAll`.
        final Iterable<LockTimeEntriesSettingsEntity> settings = lockTimeEntriesSettingsRepository.findAll();
        return stream(settings.spliterator(), false).findFirst();
    }
}
