package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.vacationtype;

import de.focusshift.zeiterfassung.absence.AbsenceTypeService;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "zeiterfassung.integration.urlaubsverwaltung.vacationtype.enabled", havingValue = "true")
@EnableConfigurationProperties(VacationTypeRabbitmqConfigurationProperties.class)
class VacationTypeRabbitmqConfiguration {

    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_VACATIONTYPE_UPDATED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.vacationtype.updated";

    @Bean
    VacationTypeHandlerRabbitmq vacationTypeHandlerRabbitmq(AbsenceTypeService absenceTypeService) {
        return new VacationTypeHandlerRabbitmq(absenceTypeService);
    }

    @ConditionalOnProperty(value = "zeiterfassung.integration.urlaubsverwaltung.vacationtype.manage-topology", havingValue = "true")
    static class ManageTopologyConfiguration {

        private final VacationTypeRabbitmqConfigurationProperties vacationTypeRabbitmqConfigurationProperties;

        ManageTopologyConfiguration(VacationTypeRabbitmqConfigurationProperties vacationTypeRabbitmqConfigurationProperties) {
            this.vacationTypeRabbitmqConfigurationProperties = vacationTypeRabbitmqConfigurationProperties;
        }

        @Bean
        public TopicExchange vacationTypeTopic() {
            return new TopicExchange(vacationTypeRabbitmqConfigurationProperties.getTopic());
        }

        @Bean
        Queue zeiterfassungUrlaubsverwatlungVacationTypeUpdatedQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_VACATIONTYPE_UPDATED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungVacationTypeUpdatedQueue() {
            final String routingKeyUpdated = vacationTypeRabbitmqConfigurationProperties.getRoutingKeyUpdated();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwatlungVacationTypeUpdatedQueue())
                .to(vacationTypeTopic())
                .with(routingKeyUpdated);
        }
    }
}
