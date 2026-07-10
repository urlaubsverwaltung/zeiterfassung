package de.focusshift.zeiterfassung.timeentry.republish;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.integration.day-locked-republish")
record DayLockedRepublishRabbitmqConfigurationProperties(
    @DefaultValue("false") boolean enabled,
    @DefaultValue("false") boolean manageTopology,
    @DefaultValue("zeiterfassung.topic") @NotEmpty String topic,
    @DefaultValue("ZE.EVENT.DAYLOCKED.REPUBLISH") @NotEmpty String routingKey
) {
}
