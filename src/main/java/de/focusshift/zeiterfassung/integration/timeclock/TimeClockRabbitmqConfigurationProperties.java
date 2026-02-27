package de.focusshift.zeiterfassung.integration.timeclock;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.integration.timeclock")
record TimeClockRabbitmqConfigurationProperties(
    @DefaultValue("false") boolean enabled,
    @DefaultValue("false") boolean manageTopology,
    @DefaultValue("zeiterfassung.topic") @NotEmpty String topic,
    @DefaultValue("ZE.EVENT.%s.TIMECLOCK.STARTED") @NotEmpty String routingKeyStarted,
    @DefaultValue("ZE.EVENT.%s.TIMECLOCK.UPDATED") @NotEmpty String routingKeyUpdated,
    @DefaultValue("ZE.EVENT.%s.TIMECLOCK.STOPPED") @NotEmpty String routingKeyStopped
) {
}
