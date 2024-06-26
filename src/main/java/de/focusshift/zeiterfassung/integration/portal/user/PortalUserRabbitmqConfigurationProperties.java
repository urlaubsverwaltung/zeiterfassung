package de.focusshift.zeiterfassung.integration.portal.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("zeiterfassung.integration.portal.user")
class PortalUserRabbitmqConfigurationProperties {

    private boolean enabled = false;

    private boolean manageTopology = false;

    private String topic = "portal.topic";

    private String routingKeyCreated = "created";
    private String routingKeyUpdated = "updated";
    private String routingKeyDeleted = "deleted";
    private String routingKeyActivated = "activated";
    private String routingKeyDeactivated = "deactivated";

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

    public String getRoutingKeyCreated() {
        return routingKeyCreated;
    }

    public void setRoutingKeyCreated(String routingKeyCreated) {
        this.routingKeyCreated = routingKeyCreated;
    }

    public String getRoutingKeyUpdated() {
        return routingKeyUpdated;
    }

    public void setRoutingKeyUpdated(String routingKeyUpdated) {
        this.routingKeyUpdated = routingKeyUpdated;
    }

    public String getRoutingKeyDeleted() {
        return routingKeyDeleted;
    }

    public void setRoutingKeyDeleted(String routingKeyDeleted) {
        this.routingKeyDeleted = routingKeyDeleted;
    }

    public String getRoutingKeyActivated() {
        return routingKeyActivated;
    }

    public void setRoutingKeyActivated(String routingKeyActivated) {
        this.routingKeyActivated = routingKeyActivated;
    }

    public String getRoutingKeyDeactivated() {
        return routingKeyDeactivated;
    }

    public void setRoutingKeyDeactivated(String routingKeyDeactivated) {
        this.routingKeyDeactivated = routingKeyDeactivated;
    }
}
