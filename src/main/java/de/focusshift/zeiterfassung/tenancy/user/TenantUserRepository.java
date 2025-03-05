package de.focusshift.zeiterfassung.tenancy.user;

import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface TenantUserRepository extends CrudRepository<TenantUserEntity, Long> {

    Optional<TenantUserEntity> findByUuid(String uuid);

    @NonNull
    List<TenantUserEntity> findAllByOrderByGivenNameAscFamilyNameAsc();

    @NonNull
    List<TenantUserEntity> findAllByUuidIsInOrderByGivenNameAscFamilyNameAsc(Collection<String> ids);

    @NonNull
    List<TenantUserEntity> findAllByIdIsInOrderByGivenNameAscFamilyNameAsc(Collection<Long> localIds);

    @NonNull
    List<TenantUserEntity> findAllByGivenNameContainingIgnoreCaseOrFamilyNameContainingIgnoreCaseOrderByGivenNameAscFamilyNameAsc(String givenNameQuery, String familyNameQuery);
}
