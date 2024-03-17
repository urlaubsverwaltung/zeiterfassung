package de.focusshift.zeiterfassung.settings;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

interface FederalStateSettingsRepository extends CrudRepository<FederalStateSettingsEntity, Long> {

    Optional<FederalStateSettingsEntity> findByTenantId(String tenantId);
}
