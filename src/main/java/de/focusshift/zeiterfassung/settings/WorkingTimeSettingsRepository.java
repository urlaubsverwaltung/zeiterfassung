package de.focusshift.zeiterfassung.settings;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
interface WorkingTimeSettingsRepository extends CrudRepository<WorkingTimeSettingsEntity, Long> {
}
