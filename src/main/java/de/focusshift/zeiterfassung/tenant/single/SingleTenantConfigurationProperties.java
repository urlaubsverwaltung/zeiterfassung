package de.focusshift.zeiterfassung.tenant.single;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties("zeiterfassung.tenant.single")
public class SingleTenantConfigurationProperties {

    /**
     * Sets the id of the default tenant, default is 'default'
     */
    @NotBlank
    private String defaultTenantId = "default";

    public String getDefaultTenantId() {
        return defaultTenantId;
    }

    public void setDefaultTenantId(String defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }
}
