package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application;

import de.focusshift.zeiterfassung.absence.AbsenceWriteService;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "zeiterfassung.integration.urlaubsverwaltung.application.enabled", havingValue = "true")
@EnableConfigurationProperties(ApplicationRabbitmqConfigurationProperties.class)
class ApplicationRabbitmqConfiguration {

    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_ALLOWED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.application.allowed";
    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_UPDATED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.application.updated";
    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CANCELLED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.application.cancelled";
    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CREATED_FROM_SICKNOTE_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.application.created_from_sicknote";

    @Bean
    ApplicationEventHandlerRabbitmq applicationEventHandlerRabbitmq(AbsenceWriteService absenceWriteService, TenantContextHolder tenantContextHolder) {
        return new ApplicationEventHandlerRabbitmq(absenceWriteService, tenantContextHolder);
    }

    @Configuration
    @ConditionalOnProperty(value = "zeiterfassung.integration.urlaubsverwaltung.application.manage-topology", havingValue = "true")
    static class ManageTopologyConfiguration {

        private final ApplicationRabbitmqConfigurationProperties applicationRabbitmqConfigurationProperties;

        ManageTopologyConfiguration(ApplicationRabbitmqConfigurationProperties applicationRabbitmqConfigurationProperties) {
            this.applicationRabbitmqConfigurationProperties = applicationRabbitmqConfigurationProperties;
        }

        @Bean
        public TopicExchange applicationTopic() {
            return new TopicExchange(applicationRabbitmqConfigurationProperties.getTopic());
        }

        @Bean
        Queue zeiterfassungUrlaubsverwaltungApplicationAllowedQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_ALLOWED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungApplicationAllowedQueue() {
            final String routingKeyAllowed = applicationRabbitmqConfigurationProperties.getRoutingKeyAllowed();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungApplicationAllowedQueue())
                .to(applicationTopic())
                .with(routingKeyAllowed);
        }

        @Bean
        Queue zeiterfassungUrlaubsverwaltungApplicationUpdatedQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_UPDATED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungApplicationUpdatedQueue() {
            final String routingKeyUpdated = applicationRabbitmqConfigurationProperties.getRoutingKeyUpdated();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungApplicationUpdatedQueue())
                .to(applicationTopic())
                .with(routingKeyUpdated);
        }

        @Bean
        Queue zeiterfassungUrlaubsverwaltungApplicationCancelledQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CANCELLED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungApplicationCancelledQueue() {
            final String routingKeyCancelled = applicationRabbitmqConfigurationProperties.getRoutingKeyCancelled();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungApplicationCancelledQueue())
                .to(applicationTopic())
                .with(routingKeyCancelled);
        }

        @Bean
        Queue zeiterfassungUrlaubsverwaltungApplicationCreatedFromSicknoteQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CREATED_FROM_SICKNOTE_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungApplicationCreatedFromSicknoteQueue() {
            final String routingKeyCreatedFromSicknote = applicationRabbitmqConfigurationProperties.getRoutingKeyCreatedFromSicknote();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungApplicationCreatedFromSicknoteQueue())
                .to(applicationTopic())
                .with(routingKeyCreatedFromSicknote);
        }
    }
}
