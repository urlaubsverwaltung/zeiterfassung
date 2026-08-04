package de.focusshift.zeiterfassung.settings;

import org.springframework.data.repository.CrudRepository;

interface TimeEntrySettingsRepository extends CrudRepository<TimeEntrySettingsEntity, Long> {
}
