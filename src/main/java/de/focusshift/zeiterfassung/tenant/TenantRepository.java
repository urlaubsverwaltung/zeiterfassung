package de.focusshift.zeiterfassung.tenant;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

interface TenantRepository extends CrudRepository<TenantEntity, Long> {

    Optional<TenantEntity> findByTenantId(String tenantId);

    @NonNull
    List<TenantEntity> findAll();

    @Modifying
    @Transactional
    void deleteByTenantId(String tenantId);
}
