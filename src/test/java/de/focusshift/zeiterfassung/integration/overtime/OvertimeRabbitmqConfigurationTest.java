package de.focusshift.zeiterfassung.integration.overtime;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OvertimeRabbitmqConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(OvertimeRabbitmqConfiguration.class);

    @Test
    void ensureNoBeansWhenOvertimeIsDisabledByDefault() {
        applicationContextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(OvertimeEventPublisherRabbitmq.class);
        });
    }

    @Test
    void ensureNoBeansWhenOvertimeIsDisabledByProperty() {
        applicationContextRunner
            .withPropertyValues("zeiterfassung.integration.overtime.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(OvertimeEventPublisherRabbitmq.class);
            });
    }

    @Test
    void ensureOvertimeEventPublisherRabbitmqBean() {
        applicationContextRunner
            .withPropertyValues(
                "zeiterfassung.integration.overtime.enabled=true",
                "zeiterfassung.integration.overtime.topic=awesome-topic",
                "zeiterfassung.integration.overtime.routing-key-entered=awesome-route"
            )
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class))
            .run(context -> {
                assertThat(context).hasSingleBean(OvertimeEventPublisherRabbitmq.class);
            });
    }

    @Test
    void ensureEnabledManageTopologyBeans() {

        applicationContextRunner
            .withPropertyValues(
                "zeiterfassung.integration.overtime.enabled=true",
                "zeiterfassung.integration.overtime.topic=awesome-topic",
                "zeiterfassung.integration.overtime.routing-key-entered=awesome-route",
                "zeiterfassung.integration.overtime.manage-topology=true"
            )
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class))
            .run(context -> {
                assertThat(context).hasSingleBean(OvertimeEventPublisherRabbitmq.class);
                assertThat(context.getBean(TopicExchange.class).getName()).isEqualTo("awesome-topic");
                assertThat(context.getBean(Queue.class)).satisfies(queue -> {
                    assertThat(queue.getName()).isEqualTo("zeiterfassung.queue.overtime.entered");
                    assertThat(queue.isDurable()).isTrue();
                });
                assertThat(context.getBean(Binding.class)).satisfies(binding -> {
                    assertThat(binding.getDestination()).isEqualTo("zeiterfassung.queue.overtime.entered");
                    assertThat(binding.getExchange()).isEqualTo("awesome-topic");
                    assertThat(binding.getRoutingKey()).isEqualTo("awesome-route");
                });
            });
    }
}
