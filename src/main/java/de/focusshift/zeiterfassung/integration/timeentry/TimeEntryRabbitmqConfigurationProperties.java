package de.focusshift.zeiterfassung.integration.timeentry;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.integration.timeentry")
record TimeEntryRabbitmqConfigurationProperties(
    @DefaultValue("false") boolean enabled,
    @DefaultValue("false") boolean manageTopology,
    @DefaultValue("zeiterfassung.topic") @NotEmpty String topic,
    @DefaultValue("ZE.EVENT.%s.TIMEENTRY.CREATED") @NotEmpty String routingKeyCreated,
    @DefaultValue("ZE.EVENT.%s.TIMEENTRY.UPDATED") @NotEmpty String routingKeyUpdated,
    @DefaultValue("ZE.EVENT.%s.TIMEENTRY.DELETED") @NotEmpty String routingKeyDeleted
) {
}
