package de.focusshift.zeiterfassung.integration.workingtime;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WorkingTimeRabbitmqConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(WorkingTimeRabbitmqConfiguration.class);

    @Test
    void ensureNoBeansWhenWorkingTimeIsDisabledByDefault() {
        applicationContextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(WorkingTimeRabbitEventPublisher.class);
        });
    }

    @Test
    void ensureNoBeansWhenWorkingTimeIsDisabledByProperty() {
        applicationContextRunner
            .withPropertyValues("zeiterfassung.integration.workingtime.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(WorkingTimeRabbitEventPublisher.class);
            });
    }

    @Test
    void ensureWorkingTimeRabbitEventPublisherBean() {
        applicationContextRunner
            .withPropertyValues(
                "zeiterfassung.integration.workingtime.enabled=true",
                "zeiterfassung.integration.workingtime.topic=awesome-topic",
                "zeiterfassung.integration.workingtime.routing-key-created=%s.awesome-route-created",
                "zeiterfassung.integration.workingtime.routing-key-updated=%s.awesome-route-updated",
                "zeiterfassung.integration.workingtime.routing-key-deleted=%s.awesome-route-deleted"
            )
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class))
            .run(context -> {
                assertThat(context).hasSingleBean(WorkingTimeRabbitEventPublisher.class);
            });
    }

    @Test
    void ensureEnabledManageTopologyBeans() {
        applicationContextRunner
            .withPropertyValues(
                "zeiterfassung.integration.workingtime.enabled=true",
                "zeiterfassung.integration.workingtime.topic=awesome-topic",
                "zeiterfassung.integration.workingtime.routing-key-created=%s.awesome-route-created",
                "zeiterfassung.integration.workingtime.routing-key-updated=%s.awesome-route-updated",
                "zeiterfassung.integration.workingtime.routing-key-deleted=%s.awesome-route-deleted",
                "zeiterfassung.integration.workingtime.manage-topology=true"
            )
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class))
            .run(context -> {
                assertThat(context).hasSingleBean(WorkingTimeRabbitEventPublisher.class);
                assertThat(context.getBean(TopicExchange.class).getName()).isEqualTo("awesome-topic");
            });
    }
}
