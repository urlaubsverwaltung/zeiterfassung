package de.focusshift.zeiterfassung.integration.overtime;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.junit.jupiter.api.Test;
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
                "zeiterfassung.integration.overtime.routing-key-entered=%s.awesome-route"
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
                "zeiterfassung.integration.overtime.routing-key-updated=%s.awesome-route-updated",
                "zeiterfassung.integration.overtime.manage-topology=true"
            )
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class))
            .run(context -> {
                assertThat(context).hasSingleBean(OvertimeRabbitEventPublisher.class);
                assertThat(context.getBean(TopicExchange.class).getName()).isEqualTo("awesome-topic");
            });
    }
}
