package de.focusshift.zeiterfassung.settings;

import org.springframework.data.repository.CrudRepository;

interface LockTimeEntriesSettingsRepository extends CrudRepository<LockTimeEntriesSettingsEntity, Long> {
}
