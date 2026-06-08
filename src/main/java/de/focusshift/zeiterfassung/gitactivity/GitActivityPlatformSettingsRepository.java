package de.focusshift.zeiterfassung.gitactivity;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
interface GitActivityPlatformSettingsRepository
    extends CrudRepository<GitActivityPlatformSettingsEntity, String> {
}
