package de.focusshift.zeiterfassung.integration.workingtime;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "zeiterfassung.integration.workingtime.enabled", havingValue = "true")
@EnableConfigurationProperties(WorkingTimeRabbitmqConfigurationProperties.class)
class WorkingTimeRabbitmqConfiguration {

    @Bean
    public WorkingTimeRabbitEventPublisher workingTimeRabbitEventPublisher(
        RabbitTemplate rabbitTemplate,
        TenantContextHolder tenantContextHolder,
        WorkingTimeRabbitmqConfigurationProperties properties
    ) {
        return new WorkingTimeRabbitEventPublisher(rabbitTemplate, tenantContextHolder, properties);
    }

    @Configuration
    @ConditionalOnProperty(value = "zeiterfassung.integration.workingtime.manage-topology", havingValue = "true")
    static class ManageTopologyConfiguration {

        private final WorkingTimeRabbitmqConfigurationProperties workingTimeRabbitmqConfigurationProperties;

        ManageTopologyConfiguration(WorkingTimeRabbitmqConfigurationProperties workingTimeRabbitmqConfigurationProperties) {
            this.workingTimeRabbitmqConfigurationProperties = workingTimeRabbitmqConfigurationProperties;
        }

        @Bean
        public TopicExchange workingTimeTopic() {
            return new TopicExchange(workingTimeRabbitmqConfigurationProperties.topic());
        }
    }
}
