package de.focusshift.zeiterfassung.security.oidc.clientregistration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.MULTI;

@Repository
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
interface OidcClientEntityRepository extends CrudRepository<OidcClientEntity, Long> {

    OidcClientEntity findByTenantId(String tenantId);

    @NonNull
    List<OidcClientEntity> findAll();
}
