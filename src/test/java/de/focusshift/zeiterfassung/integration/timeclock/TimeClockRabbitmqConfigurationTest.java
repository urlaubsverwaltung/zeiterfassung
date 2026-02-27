package de.focusshift.zeiterfassung.integration.timeclock;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TimeClockRabbitmqConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TimeClockRabbitmqConfiguration.class);

    @Test
    void ensureNoBeansWhenTimeClockIsDisabledByDefault() {
        applicationContextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(TimeClockRabbitEventPublisher.class);
        });
    }

    @Test
    void ensureNoBeansWhenTimeClockIsDisabledByProperty() {
        applicationContextRunner
            .withPropertyValues("zeiterfassung.integration.timeclock.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(TimeClockRabbitEventPublisher.class);
            });
    }

    @Test
    void ensureTimeClockRabbitEventPublisherBean() {
        applicationContextRunner
            .withPropertyValues(
                "zeiterfassung.integration.timeclock.enabled=true",
                "zeiterfassung.integration.timeclock.topic=awesome-topic",
                "zeiterfassung.integration.timeclock.routing-key-started=%s.awesome-route-started",
                "zeiterfassung.integration.timeclock.routing-key-updated=%s.awesome-route-updated",
                "zeiterfassung.integration.timeclock.routing-key-stopped=%s.awesome-route-stopped"
            )
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class))
            .run(context -> {
                assertThat(context).hasSingleBean(TimeClockRabbitEventPublisher.class);
            });
    }

    @Test
    void ensureEnabledManageTopologyBeans() {
        applicationContextRunner
            .withPropertyValues(
                "zeiterfassung.integration.timeclock.enabled=true",
                "zeiterfassung.integration.timeclock.topic=awesome-topic",
                "zeiterfassung.integration.timeclock.routing-key-started=%s.awesome-route-started",
                "zeiterfassung.integration.timeclock.routing-key-updated=%s.awesome-route-updated",
                "zeiterfassung.integration.timeclock.routing-key-stopped=%s.awesome-route-stopped",
                "zeiterfassung.integration.timeclock.manage-topology=true"
            )
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class))
            .run(context -> {
                assertThat(context).hasSingleBean(TimeClockRabbitEventPublisher.class);
                assertThat(context.getBean(TopicExchange.class).getName()).isEqualTo("awesome-topic");
            });
    }
}
