package de.focusshift.zeiterfassung.tenancy.tenant;

import java.util.List;
import java.util.Optional;

public interface TenantService {

    Optional<Tenant> getTenantByTenantId(String tenantId);

    Tenant create(String tenantId);

    Tenant disable(String tenantId);

    List<Tenant> findAllTenants();

    void delete(String tenantId);
}
