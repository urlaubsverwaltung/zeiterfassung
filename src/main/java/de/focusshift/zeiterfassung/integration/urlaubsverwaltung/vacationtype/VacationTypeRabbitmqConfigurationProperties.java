package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.vacationtype;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.integration.urlaubsverwaltung.vacationtype")
class VacationTypeRabbitmqConfigurationProperties {

    private boolean enabled = false;

    private boolean manageTopology = false;

    @NotEmpty
    private String topic = "vacationtype.topic";

    @NotEmpty
    private String routingKeyUpdated = "updated";

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

    public String getRoutingKeyUpdated() {
        return routingKeyUpdated;
    }

    public void setRoutingKeyUpdated(String routingKeyUpdated) {
        this.routingKeyUpdated = routingKeyUpdated;
    }
}
