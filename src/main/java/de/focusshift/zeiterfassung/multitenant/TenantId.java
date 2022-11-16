package de.focusshift.zeiterfassung.multitenant;

record TenantId(String tenantId) {

    private static final String TENANT_ID_REGEX_PATTTERN = "^[0-9a-fA-F]{8}$";

    boolean valid() {
        return tenantId != null && tenantId.matches(TENANT_ID_REGEX_PATTTERN);
    }
}
