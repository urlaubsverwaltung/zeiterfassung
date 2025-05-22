package de.focusshift.zeiterfassung.integration.overtime;


import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
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

    @Bean
    public OvertimeRabbitEventPublisher overtimeRabbitEventPublisher(
        RabbitTemplate rabbitTemplate,
        TenantContextHolder tenantContextHolder,
        OvertimeRabbitmqConfigurationProperties properties
    ) {
        return new OvertimeRabbitEventPublisher(rabbitTemplate, tenantContextHolder, properties);
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
    }
}
