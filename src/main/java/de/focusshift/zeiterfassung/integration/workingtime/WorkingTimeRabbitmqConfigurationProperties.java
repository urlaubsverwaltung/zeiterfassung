package de.focusshift.zeiterfassung.integration.workingtime;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.integration.workingtime")
record WorkingTimeRabbitmqConfigurationProperties(
    @DefaultValue("false") boolean enabled,
    @DefaultValue("false") boolean manageTopology,
    @DefaultValue("zeiterfassung.topic") @NotEmpty String topic,
    @DefaultValue("ZE.EVENT.%s.WORKINGTIME.CREATED") @NotEmpty String routingKeyCreated,
    @DefaultValue("ZE.EVENT.%s.WORKINGTIME.UPDATED") @NotEmpty String routingKeyUpdated,
    @DefaultValue("ZE.EVENT.%s.WORKINGTIME.DELETED") @NotEmpty String routingKeyDeleted
) {
}
