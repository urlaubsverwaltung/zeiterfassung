package de.focusshift.zeiterfassung.integration.portal.user;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantService;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "zeiterfassung.integration.portal.user.enabled", havingValue = "true")
@EnableConfigurationProperties(PortalUserRabbitmqConfigurationProperties.class)
class PortalUserRabbitmqConfiguration {

    static final String ZEITERFASSUNG_PORTAL_USER_CREATED_QUEUE = "zeiterfassung.queue.portal.user.created";
    static final String ZEITERFASSUNG_PORTAL_USER_UPDATED_QUEUE = "zeiterfassung.queue.portal.user.updated";
    static final String ZEITERFASSUNG_PORTAL_USER_DELETED_QUEUE = "zeiterfassung.queue.portal.user.deleted";
    static final String ZEITERFASSUNG_PORTAL_USER_ACTIVATED_QUEUE = "zeiterfassung.queue.portal.user.activated";
    static final String ZEITERFASSUNG_PORTAL_USER_DEACTIVATED_QUEUE = "zeiterfassung.queue.portal.user.deactivated";

    @Bean
    PortalUserEventHandlerRabbitmq portalUserEventHandlerRabbitmq(TenantContextHolder tenantContextHolder, TenantUserService tenantUserService, TenantService tenantService) {
        return new PortalUserEventHandlerRabbitmq(tenantContextHolder, tenantUserService, tenantService);
    }


    @Configuration
    @ConditionalOnProperty(value = "zeiterfassung.integration.portal.user.manage-topology", havingValue = "true")
    static class ManageTopologyConfiguration {

        private final PortalUserRabbitmqConfigurationProperties portalUserRabbitmqConfigurationProperties;

        ManageTopologyConfiguration(PortalUserRabbitmqConfigurationProperties applicationRabbitmqConfigurationProperties) {
            this.portalUserRabbitmqConfigurationProperties = applicationRabbitmqConfigurationProperties;
        }

        @Bean
        public TopicExchange portalTopic() {
            return new TopicExchange(portalUserRabbitmqConfigurationProperties.getTopic());
        }

        @Bean
        Queue zeiterfassungPortalUserCreatedQueue() {
            return new Queue(ZEITERFASSUNG_PORTAL_USER_CREATED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungPortalUserCreatedQueue() {
            final String routingKeyCreated = portalUserRabbitmqConfigurationProperties.getRoutingKeyCreated();
            return BindingBuilder.bind(zeiterfassungPortalUserCreatedQueue())
                .to(portalTopic())
                .with(routingKeyCreated);
        }

        @Bean
        Queue zeiterfassungPortalUserUpdatedQueue() {
            return new Queue(ZEITERFASSUNG_PORTAL_USER_UPDATED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungPortalUserUpdatedQueue() {
            final String routingKeyUpdated = portalUserRabbitmqConfigurationProperties.getRoutingKeyUpdated();
            return BindingBuilder.bind(zeiterfassungPortalUserUpdatedQueue())
                .to(portalTopic())
                .with(routingKeyUpdated);
        }

        @Bean
        Queue zeiterfassungPortalUserDeletedQueue() {
            return new Queue(ZEITERFASSUNG_PORTAL_USER_DELETED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungPortalUserDeletedQueue() {
            final String routingKeyDeleted = portalUserRabbitmqConfigurationProperties.getRoutingKeyDeleted();
            return BindingBuilder.bind(zeiterfassungPortalUserDeletedQueue())
                .to(portalTopic())
                .with(routingKeyDeleted);
        }

        @Bean
        Queue zeiterfassungPortalUserActivatedQueue() {
            return new Queue(ZEITERFASSUNG_PORTAL_USER_ACTIVATED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungPortalUserActivatedQueue() {
            final String routingKeyActivated = portalUserRabbitmqConfigurationProperties.getRoutingKeyActivated();
            return BindingBuilder.bind(zeiterfassungPortalUserActivatedQueue())
                .to(portalTopic())
                .with(routingKeyActivated);
        }

        @Bean
        Queue zeiterfassungPortalUserDeactivatedQueue() {
            return new Queue(ZEITERFASSUNG_PORTAL_USER_DEACTIVATED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungPortalUserDeactivatedQueue() {
            final String routingKeyDeactivated = portalUserRabbitmqConfigurationProperties.getRoutingKeyDeactivated();
            return BindingBuilder.bind(zeiterfassungPortalUserDeactivatedQueue())
                .to(portalTopic())
                .with(routingKeyDeactivated);
        }
    }
}
