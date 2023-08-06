package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.person;

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
@ConditionalOnProperty(value = "zeiterfassung.integration.urlaubsverwaltung.person.enabled", havingValue = "true")
@EnableConfigurationProperties(PersonRabbitmqConfigurationProperties.class)
class PersonRabbitmqConfiguration {

    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_CREATED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.person.created";
    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_UPDATED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.person.updated";
    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_DISABLED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.person.disabled";
    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_DELETED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.person.deleted";


    @Bean
    PersonEventHandlerRabbitmq personEventHandlerRabbitmq(TenantContextHolder tenantContextHolder, TenantService tenantService, TenantUserService tenantUserService) {
        return new PersonEventHandlerRabbitmq(tenantContextHolder, tenantService, tenantUserService);
    }

    @Configuration
    @ConditionalOnProperty(value = "zeiterfassung.integration.urlaubsverwaltung.person.manage-topology", havingValue = "true")
    static class ManageTopologyConfiguration {

        private final PersonRabbitmqConfigurationProperties personRabbitmqConfigurationProperties;

        ManageTopologyConfiguration(PersonRabbitmqConfigurationProperties personRabbitmqConfigurationProperties) {
            this.personRabbitmqConfigurationProperties = personRabbitmqConfigurationProperties;
        }

        @Bean
        public TopicExchange personTopic() {
            return new TopicExchange(personRabbitmqConfigurationProperties.getTopic());
        }

        @Bean
        Queue zeiterfassungUrlaubsverwaltungPersonCreatedQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_CREATED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungPersonCreatedQueue() {
            final String routingKey = personRabbitmqConfigurationProperties.getRoutingKeyCreated();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungPersonCreatedQueue())
                .to(personTopic())
                .with(routingKey);
        }

        @Bean
        Queue zeiterfassungUrlaubsverwaltungPersonUpdatedQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_UPDATED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungPersonUpdatedQueue() {
            final String routingKey = personRabbitmqConfigurationProperties.getRoutingKeyUpdated();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungPersonUpdatedQueue())
                .to(personTopic())
                .with(routingKey);
        }

        @Bean
        Queue zeiterfassungUrlaubsverwaltungPersonDisabledQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_DISABLED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungPersonDisabledQueue() {
            final String routingKey = personRabbitmqConfigurationProperties.getRoutingKeyDisabled();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungPersonDisabledQueue())
                .to(personTopic())
                .with(routingKey);
        }

        @Bean
        Queue zeiterfassungUrlaubsverwaltungPersonDeletedQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_DELETED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungPersonDeletedQueue() {
            final String routingKey = personRabbitmqConfigurationProperties.getRoutingKeyDeleted();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungPersonDeletedQueue())
                .to(personTopic())
                .with(routingKey);
        }


    }
}
