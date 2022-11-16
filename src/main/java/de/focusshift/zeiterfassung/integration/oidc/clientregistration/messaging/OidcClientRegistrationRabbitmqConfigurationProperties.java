package de.focusshift.zeiterfassung.integration.oidc.clientregistration.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Validated
@ConfigurationProperties("zeiterfassung.integration.oidc.client-registration")
class OidcClientRegistrationRabbitmqConfigurationProperties {

    private boolean enabled;

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

    @Deprecated
    public void setRoutingKeyDeletedTwo(String routingKeyDeletedTemplate) {
        this.routingKeyDeletedTemplate = routingKeyDeletedTemplate;
    }

    public void setRoutingKeyDeletedTemplate(String routingKeyDeletedTemplate) {
        this.routingKeyDeletedTemplate = routingKeyDeletedTemplate;
    }
}
