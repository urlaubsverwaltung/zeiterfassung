package de.focusshift.zeiterfassung.timeentry.republish;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.MULTI;

@Configuration
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
@EnableConfigurationProperties(DayLockedRepublishRabbitmqConfigurationProperties.class)
class DayLockedRepublishRabbitmqConfiguration {

    static final String ZEITERFASSUNG_DAY_LOCKED_REPUBLISH_QUEUE = "zeiterfassung.queue.day-locked-republish";

    @Configuration
    @ConditionalOnProperty(value = "zeiterfassung.integration.day-locked-republish.enabled", havingValue = "true")
    static class DayLockedRepublishListenerConfiguration {

        @Bean
        DayLockedRepublishEventHandlerRabbitmq dayLockedRepublishEventHandlerRabbitmq(DayLockedRepublishService dayLockedRepublishService) {
            return new DayLockedRepublishEventHandlerRabbitmq(dayLockedRepublishService);
        }
    }

    @Configuration
    @ConditionalOnProperty(value = "zeiterfassung.integration.day-locked-republish.manage-topology", havingValue = "true")
    static class ManageTopologyConfiguration {

        private final DayLockedRepublishRabbitmqConfigurationProperties properties;

        ManageTopologyConfiguration(DayLockedRepublishRabbitmqConfigurationProperties properties) {
            this.properties = properties;
        }

        @Bean
        TopicExchange dayLockedRepublishTopic() {
            return new TopicExchange(properties.topic());
        }

        @Bean
        Queue dayLockedRepublishQueue() {
            return new Queue(ZEITERFASSUNG_DAY_LOCKED_REPUBLISH_QUEUE, true);
        }

        @Bean
        Binding bindDayLockedRepublishQueue() {
            return BindingBuilder.bind(dayLockedRepublishQueue())
                .to(dayLockedRepublishTopic())
                .with(properties.routingKey());
        }
    }
}
