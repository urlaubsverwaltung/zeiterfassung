package de.focusshift.zeiterfassung.integration.overtimeaccount;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OvertimeAccountRabbitmqConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(OvertimeAccountRabbitmqConfiguration.class);

    @Test
    void ensureNoBeansWhenOvertimeAccountIsDisabledByDefault() {
        applicationContextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(OvertimeAccountRabbitEventPublisher.class);
        });
    }

    @Test
    void ensureNoBeansWhenOvertimeAccountIsDisabledByProperty() {
        applicationContextRunner
            .withPropertyValues("zeiterfassung.integration.overtime-account.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(OvertimeAccountRabbitEventPublisher.class);
            });
    }

    @Test
    void ensureOvertimeAccountRabbitEventPublisherBean() {
        applicationContextRunner
            .withPropertyValues(
                "zeiterfassung.integration.overtime-account.enabled=true",
                "zeiterfassung.integration.overtime-account.topic=awesome-topic",
                "zeiterfassung.integration.overtime-account.routing-key-updated=%s.awesome-route-updated"
            )
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class))
            .run(context -> {
                assertThat(context).hasSingleBean(OvertimeAccountRabbitEventPublisher.class);
            });
    }

    @Test
    void ensureEnabledManageTopologyBeans() {
        applicationContextRunner
            .withPropertyValues(
                "zeiterfassung.integration.overtime-account.enabled=true",
                "zeiterfassung.integration.overtime-account.topic=awesome-topic",
                "zeiterfassung.integration.overtime-account.routing-key-updated=%s.awesome-route-updated",
                "zeiterfassung.integration.overtime-account.manage-topology=true"
            )
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withBean(TenantContextHolder.class, () -> mock(TenantContextHolder.class))
            .run(context -> {
                assertThat(context).hasSingleBean(OvertimeAccountRabbitEventPublisher.class);
                assertThat(context.getBean(TopicExchange.class).getName()).isEqualTo("awesome-topic");
            });
    }
}
