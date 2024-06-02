package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
class SettingsService implements FederalStateSettingsService {

    private final FederalStateSettingsRepository federalStateSettingsRepository;

    SettingsService(FederalStateSettingsRepository federalStateSettingsRepository) {
        this.federalStateSettingsRepository = federalStateSettingsRepository;
    }

    private static FederalStateSettings toFederalStateSettings(FederalStateSettingsEntity federalStateSettingsEntity) {
        return new FederalStateSettings(
            federalStateSettingsEntity.getFederalState(),
            federalStateSettingsEntity.isWorksOnPublicHoliday()
        );
    }

    @Override
    public FederalStateSettings getFederalStateSettings() {
        return getFederalStateEntity()
            .map(SettingsService::toFederalStateSettings)
            .orElse(FederalStateSettings.DEFAULT);
    }

    FederalStateSettings updateFederalStateSettings(FederalState federalState, boolean worksOnPublicHoliday) {

        final FederalStateSettingsEntity entity = getFederalStateEntity().orElseGet(FederalStateSettingsEntity::new);
        entity.setFederalState(federalState);
        entity.setWorksOnPublicHoliday(worksOnPublicHoliday);

        final FederalStateSettingsEntity saved = federalStateSettingsRepository.save(entity);

        return toFederalStateSettings(saved);
    }

    private Optional<FederalStateSettingsEntity> getFederalStateEntity() {
        return federalStateSettingsRepository.findAll().stream().findFirst();
    }
}
