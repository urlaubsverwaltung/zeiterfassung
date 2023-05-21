package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.integration.urlaubsverwaltung.sicknote")
class SickNoteRabbitmqConfigurationProperties {

    private boolean enabled = false;

    private boolean manageTopology = false;

    @NotEmpty
    private String topic = "sicknote.topic";

    @NotEmpty
    private String routingKeyCancelled = "cancelled";

    @NotEmpty
    private String routingKeyCreated = "created";

    @NotEmpty
    private String routingKeyDeleted = "deleted";

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

    public String getRoutingKeyCancelled() {
        return routingKeyCancelled;
    }

    public void setRoutingKeyCancelled(String routingKeyCancelled) {
        this.routingKeyCancelled = routingKeyCancelled;
    }

    public String getRoutingKeyCreated() {
        return routingKeyCreated;
    }

    public void setRoutingKeyCreated(String routingKeyCreated) {
        this.routingKeyCreated = routingKeyCreated;
    }

    public String getRoutingKeyDeleted() {
        return routingKeyDeleted;
    }

    public void setRoutingKeyDeleted(String routingKeyDeleted) {
        this.routingKeyDeleted = routingKeyDeleted;
    }

    public String getRoutingKeyUpdated() {
        return routingKeyUpdated;
    }

    public void setRoutingKeyUpdated(String routingKeyUpdated) {
        this.routingKeyUpdated = routingKeyUpdated;
    }
}
