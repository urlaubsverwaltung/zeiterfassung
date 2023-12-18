package de.focusshift.zeiterfassung.infobanner;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "zeiterfassung.info-banner")
record InfoBannerConfigProperties(boolean enabled, @NotNull Text text) {

    record Text(@NotEmpty String de) {
    }
}
