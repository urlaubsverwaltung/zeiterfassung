package de.focusshift.zeiterfassung.tenancy.tenant;

import java.util.Optional;

public interface TenantContextHolder {

    default Optional<TenantId> getCurrentTenantId() {
        return Optional.empty();
    }

    default void setTenantId(TenantId tenantId) {

    }

    default void clear() {

    }
}
