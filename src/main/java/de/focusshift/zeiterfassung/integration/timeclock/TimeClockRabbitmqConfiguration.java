package de.focusshift.zeiterfassung.integration.timeclock;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "zeiterfassung.integration.timeclock.enabled", havingValue = "true")
@EnableConfigurationProperties(TimeClockRabbitmqConfigurationProperties.class)
class TimeClockRabbitmqConfiguration {

    @Bean
    public TimeClockRabbitEventPublisher timeClockRabbitEventPublisher(
        RabbitTemplate rabbitTemplate,
        TenantContextHolder tenantContextHolder,
        TimeClockRabbitmqConfigurationProperties properties
    ) {
        return new TimeClockRabbitEventPublisher(rabbitTemplate, tenantContextHolder, properties);
    }

    @Configuration
    @ConditionalOnProperty(value = "zeiterfassung.integration.timeclock.manage-topology", havingValue = "true")
    static class ManageTopologyConfiguration {

        private final TimeClockRabbitmqConfigurationProperties timeClockRabbitmqConfigurationProperties;

        ManageTopologyConfiguration(TimeClockRabbitmqConfigurationProperties timeClockRabbitmqConfigurationProperties) {
            this.timeClockRabbitmqConfigurationProperties = timeClockRabbitmqConfigurationProperties;
        }

        @Bean
        public TopicExchange timeClockTopic() {
            return new TopicExchange(timeClockRabbitmqConfigurationProperties.topic());
        }
    }
}
