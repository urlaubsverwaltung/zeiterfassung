package de.focusshift.zeiterfassung.integration.timeentry;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TimeEntryRabbitmqConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TimeEntryRabbitmqConfiguration.class);

    @Test
    void ensureNoBeansWhenTimeEntryIsDisabledByDefault() {
        applicationContextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(TimeEntryRabbitEventPublisher.class);
        });
    }

    @Test
    void ensureNoBeansWhenTimeEntryIsDisabledByProperty() {
        applicationContextRunner
            .withPropertyValues("zeiterfassung.integration.timeentry.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(TimeEntryRabbitEventPublisher.class);
            });
    }

    @Test
    void ensureTimeEntryRabbitEventPublisherBean() {
        applicationContextRunner
            .withPropertyValues(
                "zeiterfassung.integration.timeentry.enabled=true",
                "zeiterfassung.integration.timeentry.topic=awesome-topic",
                "zeiterfassung.integration.timeentry.routing-key-created=%s.awesome-route-created",
                "zeiterfassung.integration.timeentry.routing-key-updated=%s.awesome-route-updated",
                "zeiterfassung.integration.timeentry.routing-key-deleted=%s.awesome-route-deleted"
            )
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class))
            .run(context -> {
                assertThat(context).hasSingleBean(TimeEntryRabbitEventPublisher.class);
            });
    }

    @Test
    void ensureEnabledManageTopologyBeans() {
        applicationContextRunner
            .withPropertyValues(
                "zeiterfassung.integration.timeentry.enabled=true",
                "zeiterfassung.integration.timeentry.topic=awesome-topic",
                "zeiterfassung.integration.timeentry.routing-key-created=%s.awesome-route-created",
                "zeiterfassung.integration.timeentry.routing-key-updated=%s.awesome-route-updated",
                "zeiterfassung.integration.timeentry.routing-key-deleted=%s.awesome-route-deleted",
                "zeiterfassung.integration.timeentry.manage-topology=true"
            )
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class))
            .run(context -> {
                assertThat(context).hasSingleBean(TimeEntryRabbitEventPublisher.class);
                assertThat(context.getBean(TopicExchange.class).getName()).isEqualTo("awesome-topic");
            });
    }
}
