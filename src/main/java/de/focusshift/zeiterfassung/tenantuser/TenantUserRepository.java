package de.focusshift.zeiterfassung.tenantuser;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

interface TenantUserRepository extends CrudRepository<TenantUserEntity, Long> {

    Optional<TenantUserEntity> findByUuid(String uuid);

    @NonNull
    List<TenantUserEntity> findAll();

    Page<TenantUserEntity> findAll(Pageable pageable);
}
