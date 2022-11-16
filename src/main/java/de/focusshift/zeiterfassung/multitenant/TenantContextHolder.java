package de.focusshift.zeiterfassung.multitenant;

import java.util.Optional;

public interface TenantContextHolder {

    Optional<TenantId> getCurrentTenantId();
}
