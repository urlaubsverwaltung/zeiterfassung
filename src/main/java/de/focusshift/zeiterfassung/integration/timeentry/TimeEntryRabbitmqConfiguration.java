package de.focusshift.zeiterfassung.integration.timeentry;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "zeiterfassung.integration.timeentry.enabled", havingValue = "true")
@EnableConfigurationProperties(TimeEntryRabbitmqConfigurationProperties.class)
class TimeEntryRabbitmqConfiguration {

    @Bean
    public TimeEntryRabbitEventPublisher timeEntryRabbitEventPublisher(
        RabbitTemplate rabbitTemplate,
        TenantContextHolder tenantContextHolder,
        TimeEntryRabbitmqConfigurationProperties properties
    ) {
        return new TimeEntryRabbitEventPublisher(rabbitTemplate, tenantContextHolder, properties);
    }

    @Configuration
    @ConditionalOnProperty(value = "zeiterfassung.integration.timeentry.manage-topology", havingValue = "true")
    static class ManageTopologyConfiguration {

        private final TimeEntryRabbitmqConfigurationProperties timeEntryRabbitmqConfigurationProperties;

        ManageTopologyConfiguration(TimeEntryRabbitmqConfigurationProperties timeEntryRabbitmqConfigurationProperties) {
            this.timeEntryRabbitmqConfigurationProperties = timeEntryRabbitmqConfigurationProperties;
        }

        @Bean
        public TopicExchange timeEntryTopic() {
            return new TopicExchange(timeEntryRabbitmqConfigurationProperties.topic());
        }
    }
}
