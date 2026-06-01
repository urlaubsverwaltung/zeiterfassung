package de.focusshift.zeiterfassung.user;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface UserSettingsRepository extends CrudRepository<UserSettingsEntity, Long> {

    Optional<UserSettingsEntity> findByTenantUserLocalId(Long tenantUserLocalId);

    List<UserSettingsEntity> findByGithubLoginVerifiedTrue();
}
