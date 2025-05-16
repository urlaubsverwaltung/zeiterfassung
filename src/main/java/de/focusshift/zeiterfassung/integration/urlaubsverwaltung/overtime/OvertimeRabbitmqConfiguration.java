package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.overtime;


import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "zeiterfassung.integration.urlaubsverwaltung.overtime.enabled", havingValue = "true")
@EnableConfigurationProperties(OvertimeRabbitmqConfigurationProperties.class)
class OvertimeRabbitmqConfiguration {

    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_OVERTIME_ENTERED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.overtime.entered";
    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_OVERTIME_UPDATED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.overtime.updated";

    @Bean
    public OvertimeEventPublisherRabbitmq overtimeEventPublisherRabbitmq(
        RabbitTemplate rabbitTemplate,
        TenantContextHolder tenantContextHolder,
        OvertimeRabbitmqConfigurationProperties properties
    ) {
        return new OvertimeEventPublisherRabbitmq(rabbitTemplate, tenantContextHolder, properties);
    }

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
        Queue zeiterfassungUrlaubsverwaltungOvertimeUpdatedQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_OVERTIME_UPDATED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungOvertimeEnteredQueue() {
            final String routingKeyEntered = overtimeRabbitmqConfigurationProperties.getRoutingKeyEntered();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungOvertimeEnteredQueue())
                .to(overtimeTopic())
                .with(routingKeyEntered);
        }

        @Bean
        Binding bindZeiterfassungOvertimeUpdatedQueue() {
            final String routingKeyUpdated = overtimeRabbitmqConfigurationProperties.getRoutingKeyUpdated();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungOvertimeUpdatedQueue())
                .to(overtimeTopic())
                .with(routingKeyUpdated);
        }
    }
}
