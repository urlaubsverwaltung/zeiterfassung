package de.focusshift.zeiterfassung.integration.overtimeaccount;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "zeiterfassung.integration.overtime-account.enabled", havingValue = "true")
@EnableConfigurationProperties(OvertimeAccountRabbitmqConfigurationProperties.class)
class OvertimeAccountRabbitmqConfiguration {

    @Bean
    public OvertimeAccountRabbitEventPublisher overtimeAccountRabbitEventPublisher(
        RabbitTemplate rabbitTemplate,
        TenantContextHolder tenantContextHolder,
        OvertimeAccountRabbitmqConfigurationProperties properties
    ) {
        return new OvertimeAccountRabbitEventPublisher(rabbitTemplate, tenantContextHolder, properties);
    }

    @Configuration
    @ConditionalOnProperty(value = "zeiterfassung.integration.overtime-account.manage-topology", havingValue = "true")
    static class ManageTopologyConfiguration {

        private final OvertimeAccountRabbitmqConfigurationProperties overtimeAccountRabbitmqConfigurationProperties;

        ManageTopologyConfiguration(OvertimeAccountRabbitmqConfigurationProperties overtimeAccountRabbitmqConfigurationProperties) {
            this.overtimeAccountRabbitmqConfigurationProperties = overtimeAccountRabbitmqConfigurationProperties;
        }

        @Bean
        public TopicExchange overtimeAccountTopic() {
            return new TopicExchange(overtimeAccountRabbitmqConfigurationProperties.topic());
        }
    }
}
