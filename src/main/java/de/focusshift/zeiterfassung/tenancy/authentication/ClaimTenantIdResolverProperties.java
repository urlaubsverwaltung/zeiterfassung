package de.focusshift.zeiterfassung.tenancy.authentication;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.tenant.resolvers.claim")
public class ClaimTenantIdResolverProperties {

    private boolean enabled = true;

    @NotBlank
    private String claimName = "tenant_id";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClaimName() {
        return claimName;
    }

    public void setClaimName(String claimName) {
        this.claimName = claimName;
    }
}
