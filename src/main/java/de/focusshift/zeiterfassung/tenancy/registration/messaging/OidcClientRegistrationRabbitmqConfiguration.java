package de.focusshift.zeiterfassung.tenancy.registration.messaging;

import de.focusshift.zeiterfassung.tenancy.registration.TenantRegistrationService;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(TenantRegistrationService.class)
@ConditionalOnProperty(value = "zeiterfassung.tenant.registration.rabbitmq.enabled", havingValue = "true")
@EnableConfigurationProperties(OidcClientRegistrationRabbitmqConfigurationProperties.class)
class OidcClientRegistrationRabbitmqConfiguration {

    static final String ZEITERFASSUNG_QUEUE_OIDC_CLIENT_CREATED_CONSUMER = "zeiterfassung.queue.oidc.client-registration.created.consumer";
    static final String ZEITERFASSUNG_QUEUE_OIDC_CLIENT_DELETED_CONSUMER = "zeiterfassung.queue.oidc.client-registration.deleted.consumer";

    @Bean
    OidcClientRegistrationEventHandlerRabbitmq oidcClientEventHandlerRabbitmq(TenantRegistrationService tenantRegistrationService) {
        return new OidcClientRegistrationEventHandlerRabbitmq(tenantRegistrationService);
    }

    @Configuration
    @ConditionalOnProperty(value = "zeiterfassung.security.oidc.client-registration.rabbitmq.manage-topology", havingValue = "true")
    static class ManageTopologyConfiguration {

        private static final String RABBITMQ_WILDCARD_SELECTOR = "*";

        private final OidcClientRegistrationRabbitmqConfigurationProperties oidcClientRegistrationRabbitmqConfigurationProperties;

        ManageTopologyConfiguration(OidcClientRegistrationRabbitmqConfigurationProperties oidcClientRegistrationRabbitmqConfigurationProperties) {
            this.oidcClientRegistrationRabbitmqConfigurationProperties = oidcClientRegistrationRabbitmqConfigurationProperties;
        }

        @Bean
        TopicExchange oidcClientTopic() {
            return new TopicExchange(oidcClientRegistrationRabbitmqConfigurationProperties.getTopic(), true, false);
        }

        @Bean
        Queue zeiterfassungOidcClientCreatedConsumerQueue() {
            return new Queue(ZEITERFASSUNG_QUEUE_OIDC_CLIENT_CREATED_CONSUMER, true);
        }

        @Bean
        Binding bindOidcClientCreatedConsumerQueue() {
            final String routingKeyCreated = oidcClientRegistrationRabbitmqConfigurationProperties.getRoutingKeyCreatedTemplate()
                .formatted(RABBITMQ_WILDCARD_SELECTOR);
            return BindingBuilder.bind(zeiterfassungOidcClientCreatedConsumerQueue())
                .to(oidcClientTopic())
                .with(routingKeyCreated.formatted(RABBITMQ_WILDCARD_SELECTOR));
        }

        @Bean
        Queue zeiterfassungOidcClientDeletedConsumerQueue() {
            return new Queue(ZEITERFASSUNG_QUEUE_OIDC_CLIENT_DELETED_CONSUMER, true);
        }

        @Bean
        Binding bindOidcClientDeletedConsumerQueue() {
            final String routingKeyDeleted = oidcClientRegistrationRabbitmqConfigurationProperties.getRoutingKeyDeletedTemplate()
                .formatted(RABBITMQ_WILDCARD_SELECTOR);
            return BindingBuilder.bind(zeiterfassungOidcClientDeletedConsumerQueue())
                .to(oidcClientTopic())
                .with(routingKeyDeleted);
        }
    }
}
