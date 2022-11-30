package de.focusshift.zeiterfassung.tenancy.user;

import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

interface TenantUserRepository extends CrudRepository<TenantUserEntity, Long> {

    Optional<TenantUserEntity> findByUuid(String uuid);

    @NonNull
    List<TenantUserEntity> findAll();
}
