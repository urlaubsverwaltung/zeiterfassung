package de.focusshift.zeiterfassung.tenant;

interface TenantAware {

    String getTenantId();

    void setTenantId(String tenantId);
}
