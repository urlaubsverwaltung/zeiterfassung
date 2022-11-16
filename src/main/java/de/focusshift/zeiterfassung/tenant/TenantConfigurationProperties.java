package de.focusshift.zeiterfassung.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

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

    private Registration registration = new Registration();

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    public static class Registration {

        /**
         * to enable or disable the generation of tenants based on the oidc clients
         */
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
