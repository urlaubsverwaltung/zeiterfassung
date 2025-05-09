package de.focusshift.zeiterfassung.integration.overtime;


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
@ConditionalOnProperty(value = "zeiterfassung.integration.overtime.enabled", havingValue = "true")
@EnableConfigurationProperties(OvertimeRabbitmqConfigurationProperties.class)
class OvertimeRabbitmqConfiguration {

    static final String ZEITERFASSUNG_OVERTIME_ENTERED_QUEUE = "zeiterfassung.queue.overtime.entered";

    @Bean
    public OvertimeEventPublisherRabbitmq overtimeEventPublisherRabbitmq(
        RabbitTemplate rabbitTemplate,
        TenantContextHolder tenantContextHolder,
        OvertimeRabbitmqConfigurationProperties properties
    ) {
        return new OvertimeEventPublisherRabbitmq(rabbitTemplate, tenantContextHolder, properties);
    }

    @Configuration
    @ConditionalOnProperty(value = "zeiterfassung.integration.overtime.manage-topology", havingValue = "true")
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
        Queue zeiterfassungOvertimeEnteredQueue() {
            return new Queue(ZEITERFASSUNG_OVERTIME_ENTERED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungApplicationAllowedQueue() {
            final String routingKeyEntered = overtimeRabbitmqConfigurationProperties.getRoutingKeyEntered();
            return BindingBuilder.bind(zeiterfassungOvertimeEnteredQueue())
                .to(overtimeTopic())
                .with(routingKeyEntered);
        }
    }
}
