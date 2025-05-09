package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.overtime;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.integration.urlaubsverwaltung.overtime")
class OvertimeRabbitmqConfigurationProperties {

    private boolean enabled = false;

    private boolean manageTopology = false;

    @NotEmpty
    private String topic = "overtime.topic";

    @NotEmpty
    private String routingKeyEntered = "entered";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isManageTopology() {
        return manageTopology;
    }

    public void setManageTopology(boolean manageTopology) {
        this.manageTopology = manageTopology;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getRoutingKeyEntered() {
        return routingKeyEntered;
    }

    public void setRoutingKeyEntered(String routingKeyEntered) {
        this.routingKeyEntered = routingKeyEntered;
    }
}
