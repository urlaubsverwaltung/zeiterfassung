package de.focusshift.zeiterfassung.tenancy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties("zeiterfassung.tenant")
public class TenantConfigurationProperties {

    public static final String SINGLE = "single";
    public static final String MULTI = "multi";

    public enum Mode {
        SINGLE,
        MULTI
    }

    @NotNull
    private Mode mode = Mode.SINGLE;

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }
}
