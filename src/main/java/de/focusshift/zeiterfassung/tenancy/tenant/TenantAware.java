package de.focusshift.zeiterfassung.tenancy.tenant;

interface TenantAware {

    String getTenantId();

    void setTenantId(String tenantId);
}
