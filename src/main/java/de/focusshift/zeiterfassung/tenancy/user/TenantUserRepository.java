package de.focusshift.zeiterfassung.tenancy.user;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
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
    @Query("select u from TenantUserEntity u where cast(strpos(lower(concat(u.givenName,' ',u.familyName)), lower(:query)) AS INTEGER) > 0 ORDER BY u.familyName desc")
    List<TenantUserEntity> findAllByNiceNameContainingIgnoreCaseOrderByGivenNameAscFamilyNameAsc(@Param("query") String query);
}
