package de.focusshift.zeiterfassung.multitenant;

public interface TenantAware {

    String getTenantId();

    void setTenantId(String tenantId);
}
