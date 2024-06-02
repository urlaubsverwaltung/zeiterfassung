package de.focusshift.zeiterfassung.settings;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

interface FederalStateSettingsRepository extends CrudRepository<FederalStateSettingsEntity, Long> {
    List<FederalStateSettingsEntity> findAll();
}
