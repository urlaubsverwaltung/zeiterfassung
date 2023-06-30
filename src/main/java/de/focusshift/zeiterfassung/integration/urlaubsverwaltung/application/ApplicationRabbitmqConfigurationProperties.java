package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.integration.urlaubsverwaltung.application")
class ApplicationRabbitmqConfigurationProperties {

    private boolean enabled = false;

    private boolean manageTopology = false;

    @NotEmpty
    private String topic = "application.topic";

    @NotEmpty
    private String routingKeyAllowed = "allowed";

    @NotEmpty
    private String routingKeyCancelled = "cancelled";

    @NotEmpty
    private String routingKeyDeleted = "deleted";

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

    public String getRoutingKeyAllowed() {
        return routingKeyAllowed;
    }

    public void setRoutingKeyAllowed(String routingKeyAllowed) {
        this.routingKeyAllowed = routingKeyAllowed;
    }

    public String getRoutingKeyCancelled() {
        return routingKeyCancelled;
    }

    public void setRoutingKeyCancelled(String routingKeyCancelled) {
        this.routingKeyCancelled = routingKeyCancelled;
    }

    public String getRoutingKeyDeleted() {
        return routingKeyDeleted;
    }

    public void setRoutingKeyDeleted(String routingKeyDeleted) {
        this.routingKeyDeleted = routingKeyDeleted;
    }
}
