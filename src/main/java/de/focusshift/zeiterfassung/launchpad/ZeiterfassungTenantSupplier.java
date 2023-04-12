package de.focusshift.zeiterfassung.launchpad;

import de.focus_shift.launchpad.tenancy.TenantSupplier;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;

public class ZeiterfassungTenantSupplier implements TenantSupplier {

    private final TenantContextHolder tenantContextHolder;

    public ZeiterfassungTenantSupplier(TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    public String get() {
        return tenantContextHolder.getCurrentTenantId().map(TenantId::tenantId).orElseThrow();
    }
}
