package de.focusshift.zeiterfassung.tenancy.registration.web;

class TenantRegistrationDTO {

    private String tenantId;
    private String oidcClientSecret;

    public TenantRegistrationDTO() {
    }

    public TenantRegistrationDTO(String tenantId, String oidcClientSecret) {
        this.tenantId = tenantId;
        this.oidcClientSecret = oidcClientSecret;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getOidcClientSecret() {
        return oidcClientSecret;
    }

    public void setOidcClientSecret(String oidcClientSecret) {
        this.oidcClientSecret = oidcClientSecret;
    }

}
