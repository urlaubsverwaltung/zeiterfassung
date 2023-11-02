package de.focusshift.zeiterfassung.tenancy.tenant;

import java.util.Optional;

public interface TenantContextHolder {

    Optional<TenantId> getCurrentTenantId();

    void setTenantId(TenantId tenantId);

    void clear();
}
