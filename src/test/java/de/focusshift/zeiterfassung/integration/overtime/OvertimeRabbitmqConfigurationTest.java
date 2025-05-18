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
            assertThat(context).doesNotHaveBean(OvertimeRabbitEventPublisher.class);
        });
    }

    @Test
    void ensureNoBeansWhenOvertimeIsDisabledByProperty() {
        applicationContextRunner
            .withPropertyValues("zeiterfassung.integration.overtime.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(OvertimeRabbitEventPublisher.class);
            });
    }

    @Test
    void ensureOvertimeRabbitEventPublisherBean() {
        applicationContextRunner
            .withPropertyValues(
                "zeiterfassung.integration.overtime.enabled=true",
                "zeiterfassung.integration.overtime.topic=awesome-topic",
                "zeiterfassung.integration.overtime.routing-key-entered=awesome-route"
            )
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class))
            .run(context -> {
                assertThat(context).hasSingleBean(OvertimeRabbitEventPublisher.class);
            });
    }

    @Test
    void ensureEnabledManageTopologyBeans() {

        applicationContextRunner
            .withPropertyValues(
                "zeiterfassung.integration.overtime.enabled=true",
                "zeiterfassung.integration.overtime.topic=awesome-topic",
                "zeiterfassung.integration.overtime.routing-key-entered=awesome-route",
                "zeiterfassung.integration.overtime.routing-key-updated=awesome-route-updated",
                "zeiterfassung.integration.overtime.manage-topology=true"
            )
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class))
            .run(context -> {
                assertThat(context).hasSingleBean(OvertimeRabbitEventPublisher.class);
                assertThat(context.getBean(TopicExchange.class).getName()).isEqualTo("awesome-topic");
                assertThat(context.getBean("zeiterfassungOvertimeEnteredQueue", Queue.class)).satisfies(queue -> {
                    assertThat(queue.getName()).isEqualTo("zeiterfassung.queue.overtime.entered");
                    assertThat(queue.isDurable()).isTrue();
                });
                assertThat(context.getBean("zeiterfassungOvertimeUpdatedQueue", Queue.class)).satisfies(queue -> {
                    assertThat(queue.getName()).isEqualTo("zeiterfassung.queue.overtime.updated");
                    assertThat(queue.isDurable()).isTrue();
                });
                assertThat(context.getBean("bindZeiterfassungOvertimeEnteredQueue", Binding.class)).satisfies(binding -> {
                    assertThat(binding.getDestination()).isEqualTo("zeiterfassung.queue.overtime.entered");
                    assertThat(binding.getExchange()).isEqualTo("awesome-topic");
                    assertThat(binding.getRoutingKey()).isEqualTo("awesome-route");
                });
                assertThat(context.getBean("bindZeiterfassungOvertimeUpdatedQueue", Binding.class)).satisfies(binding -> {
                    assertThat(binding.getDestination()).isEqualTo("zeiterfassung.queue.overtime.updated");
                    assertThat(binding.getExchange()).isEqualTo("awesome-topic");
                    assertThat(binding.getRoutingKey()).isEqualTo("awesome-route-updated");
                });
            });
    }
}
