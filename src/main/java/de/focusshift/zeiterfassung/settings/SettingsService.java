package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
class SettingsService implements FederalStateSettingsService {

    private final FederalStateSettingsRepository federalStateSettingsRepository;
    private final TenantContextHolder tenantContextHolder;

    SettingsService(FederalStateSettingsRepository federalStateSettingsRepository, TenantContextHolder tenantContextHolder) {
        this.federalStateSettingsRepository = federalStateSettingsRepository;
        this.tenantContextHolder = tenantContextHolder;
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

        final TenantId tenantId = tenantContextHolder.getCurrentTenantId()
            .orElseThrow(() -> new IllegalStateException("expected a tenantId to exist."));

        return federalStateSettingsRepository.findByTenantId(tenantId.tenantId());
    }

    private static FederalStateSettings toFederalStateSettings(FederalStateSettingsEntity federalStateSettingsEntity) {
        return new FederalStateSettings(
            federalStateSettingsEntity.getFederalState(),
            federalStateSettingsEntity.isWorksOnPublicHoliday()
        );
    }
}
