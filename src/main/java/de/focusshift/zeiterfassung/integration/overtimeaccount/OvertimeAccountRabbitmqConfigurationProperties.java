package de.focusshift.zeiterfassung.integration.overtimeaccount;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.integration.overtime-account")
record OvertimeAccountRabbitmqConfigurationProperties(
    @DefaultValue("false") boolean enabled,
    @DefaultValue("false") boolean manageTopology,
    @DefaultValue("zeiterfassung.topic") @NotEmpty String topic,
    @DefaultValue("ZE.EVENT.%s.OVERTIMEACCOUNT.UPDATED") @NotEmpty String routingKeyUpdated
) {
}
