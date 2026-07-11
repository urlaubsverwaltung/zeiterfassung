package de.focusshift.zeiterfassung.tenancy.authentication;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.tenant.resolvers.legacy")
public class LegacyTenantIdResolverProperties {

    private boolean enabled = true;

    @NotBlank
    private String tenantIdPattern = "^[0-9a-f]{8}$";

    @NotBlank
    private String issuerTenantMarker = "realms";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTenantIdPattern() {
        return tenantIdPattern;
    }

    public void setTenantIdPattern(String tenantIdPattern) {
        this.tenantIdPattern = tenantIdPattern;
    }

    public String getIssuerTenantMarker() {
        return issuerTenantMarker;
    }

    public void setIssuerTenantMarker(String issuerTenantMarker) {
        this.issuerTenantMarker = issuerTenantMarker;
    }
}
