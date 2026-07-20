package de.focusshift.zeiterfassung.security.oidc.clientregistration;

import de.focusshift.zeiterfassung.tenancy.configuration.multi.ConditionalOnMultiTenantMode;

import org.jspecify.annotations.NonNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnMultiTenantMode
interface OidcClientEntityRepository extends CrudRepository<OidcClientEntity, Long> {

    OidcClientEntity findByTenantId(String tenantId);

    @NonNull
    List<OidcClientEntity> findAll();
}
