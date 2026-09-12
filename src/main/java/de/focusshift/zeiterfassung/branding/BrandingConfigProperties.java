package de.focusshift.zeiterfassung.branding;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "zeiterfassung.branding")
public record BrandingConfigProperties(@NotEmpty String name) {
}
