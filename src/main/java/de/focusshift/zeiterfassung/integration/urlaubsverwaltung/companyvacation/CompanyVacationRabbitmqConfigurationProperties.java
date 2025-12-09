package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.companyvacation;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.integration.urlaubsverwaltung.companyvacation")
class CompanyVacationRabbitmqConfigurationProperties {

    private boolean enabled = false;

    private boolean manageTopology = false;

    @NotEmpty
    private String topic = "companyvacation.topic";

    @NotEmpty
    private String routingKeyPublished = "published";

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

    public String getRoutingKeyPublished() {
        return routingKeyPublished;
    }

    public void setRoutingKeyPublished(String routingKeyPublished) {
        this.routingKeyPublished = routingKeyPublished;
    }

    public String getRoutingKeyDeleted() {
        return routingKeyDeleted;
    }

    public void setRoutingKeyDeleted(String routingKeyDeleted) {
        this.routingKeyDeleted = routingKeyDeleted;
    }
}
