package de.focusshift.zeiterfassung.tenancy.registration.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@Validated
@ConfigurationProperties("zeiterfassung.tenant.registration.rabbitmq")
class OidcClientRegistrationRabbitmqConfigurationProperties {

    private boolean enabled = false;

    private boolean manageTopology = false;

    @NotEmpty
    private String topic = "oidc_provider.topic";

    @NotEmpty
    private String routingKeyCreatedTemplate = "OIDC_PROVIDER.%s.ZEITERFASSUNG.OIDC_CLIENT.CREATED";

    @NotEmpty
    private String routingKeyDeletedTemplate = "OIDC_PROVIDER.%s.ZEITERFASSUNG.OIDC_CLIENT.DELETED";

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

    public String getRoutingKeyCreatedTemplate() {
        return routingKeyCreatedTemplate;
    }

    public void setRoutingKeyCreatedTemplate(String routingKeyCreatedTemplate) {
        this.routingKeyCreatedTemplate = routingKeyCreatedTemplate;
    }

    public String getRoutingKeyDeletedTemplate() {
        return routingKeyDeletedTemplate;
    }

    public void setRoutingKeyDeletedTemplate(String routingKeyDeletedTemplate) {
        this.routingKeyDeletedTemplate = routingKeyDeletedTemplate;
    }
}
