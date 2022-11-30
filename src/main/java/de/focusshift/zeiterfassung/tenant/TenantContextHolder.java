package de.focusshift.zeiterfassung.tenant;

import java.util.Optional;

public interface TenantContextHolder {

    Optional<TenantId> getCurrentTenantId();
}
