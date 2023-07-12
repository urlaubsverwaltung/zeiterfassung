package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote;

import de.focusshift.zeiterfassung.absence.AbsenceWriteService;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "zeiterfassung.integration.urlaubsverwaltung.sicknote.enabled", havingValue = "true")
@EnableConfigurationProperties(SickNoteRabbitmqConfigurationProperties.class)
class SickNoteRabbitmqConfiguration {

    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CANCELLED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.sicknote.cancelled";
    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CREATED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.sicknote.created";
    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_UPDATED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.sicknote.updated";

    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CONVERTED_TO_APPLICATION_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.sicknote.converted_to_application";


    @Bean
    SickNoteEventHandlerRabbitmq sickNoteEventHandlerRabbitmq(AbsenceWriteService absenceWriteService) {
        return new SickNoteEventHandlerRabbitmq(absenceWriteService);
    }

    @Configuration
    @ConditionalOnProperty(value = "zeiterfassung.integration.urlaubsverwaltung.sicknote.manage-topology", havingValue = "true")
    static class ManageTopologyConfiguration {

        private final SickNoteRabbitmqConfigurationProperties sickNoteRabbitmqConfigurationProperties;

        ManageTopologyConfiguration(SickNoteRabbitmqConfigurationProperties sickNoteRabbitmqConfigurationProperties) {
            this.sickNoteRabbitmqConfigurationProperties = sickNoteRabbitmqConfigurationProperties;
        }

        @Bean
        public TopicExchange sickNoteTopic() {
            return new TopicExchange(sickNoteRabbitmqConfigurationProperties.getTopic());
        }

        @Bean
        Queue zeiterfassungUrlaubsverwaltungSickNoteCancelledQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CANCELLED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungSickNoteCancelledQueue() {
            final String routingKeyCancelled = sickNoteRabbitmqConfigurationProperties.getRoutingKeyCancelled();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungSickNoteCancelledQueue())
                .to(sickNoteTopic())
                .with(routingKeyCancelled);
        }

        @Bean
        Queue zeiterfassungUrlaubsverwaltungSickNoteCreatedQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CREATED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungSickNoteCreatedQueue() {
            final String routingKeyCreated = sickNoteRabbitmqConfigurationProperties.getRoutingKeyCreated();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungSickNoteCreatedQueue())
                .to(sickNoteTopic())
                .with(routingKeyCreated);
        }

        @Bean
        Queue zeiterfassungUrlaubsverwaltungSickNoteUpdatedQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_UPDATED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungSickNoteUpdatedQueue() {
            final String routingKeyUpdated = sickNoteRabbitmqConfigurationProperties.getRoutingKeyUpdated();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungSickNoteUpdatedQueue())
                .to(sickNoteTopic())
                .with(routingKeyUpdated);
        }

        @Bean
        Queue zeiterfassungUrlaubsverwaltungSickNoteConvertedToApplicationQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CONVERTED_TO_APPLICATION_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungSickNoteConvertedToApplicationQueue() {
            final String routingKeyUpdated = sickNoteRabbitmqConfigurationProperties.getRoutingKeyConvertedToApplication();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungSickNoteConvertedToApplicationQueue())
                    .to(sickNoteTopic())
                    .with(routingKeyUpdated);
        }


    }
}
