package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.overtime;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "zeiterfassung.integration.urlaubsverwaltung.overtime.enabled", havingValue = "true")
@EnableConfigurationProperties(OvertimeRabbitmqConfigurationProperties.class)
class OvertimeRabbitmqConfiguration {

    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_OVERTIME_ENTERED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.overtime.entered";

    @Configuration
    @ConditionalOnProperty(value = "zeiterfassung.integration.urlaubsverwaltung.overtime.manage-topology", havingValue = "true")
    static class ManageTopologyConfiguration {

        private final OvertimeRabbitmqConfigurationProperties overtimeRabbitmqConfigurationProperties;

        ManageTopologyConfiguration(OvertimeRabbitmqConfigurationProperties overtimeRabbitmqConfigurationProperties) {
            this.overtimeRabbitmqConfigurationProperties = overtimeRabbitmqConfigurationProperties;
        }

        @Bean
        public TopicExchange overtimeTopic() {
            return new TopicExchange(overtimeRabbitmqConfigurationProperties.getTopic());
        }

        @Bean
        Queue zeiterfassungUrlaubsverwaltungOvertimeEnteredQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_OVERTIME_ENTERED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungApplicationAllowedQueue() {
            final String routingKeyEntered = overtimeRabbitmqConfigurationProperties.getRoutingKeyEntered();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungOvertimeEnteredQueue())
                .to(overtimeTopic())
                .with(routingKeyEntered);
        }
    }
}
