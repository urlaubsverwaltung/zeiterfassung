package de.focusshift.zeiterfassung.tenancy.tenant;

import java.util.Optional;

public interface TenantContextHolder {

    Optional<TenantId> getCurrentTenantId();
}
