package de.focusshift.zeiterfassung.integration.overtime;

import de.focusshift.zeiterfassung.overtime.OvertimeHours;
import de.focusshift.zeiterfassung.overtime.events.UserHasWorkedOvertimeEvent;
import de.focusshift.zeiterfassung.overtime.events.UserHasWorkedOvertimeUpdatedEvent;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.util.UUID;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

class OvertimeRabbitEventPublisher {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final RabbitTemplate rabbitTemplate;
    private final TenantContextHolder tenantContextHolder;
    private final OvertimeRabbitmqConfigurationProperties overtimeRabbitmqConfigurationProperties;

    OvertimeRabbitEventPublisher(
        RabbitTemplate rabbitTemplate,
        TenantContextHolder tenantContextHolder,
        OvertimeRabbitmqConfigurationProperties overtimeRabbitmqConfigurationProperties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.tenantContextHolder = tenantContextHolder;
        this.overtimeRabbitmqConfigurationProperties = overtimeRabbitmqConfigurationProperties;
    }

    @Async
    @EventListener
    public void publishUserHasWorkedOvertime(UserHasWorkedOvertimeEvent event) {

        tenantContextHolder.getCurrentTenantId().ifPresentOrElse(
            tenantId -> publishRabbitOvertime(event, tenantId),
            () -> LOG.error("Cannot publish rabbit event {} without tenantId.", event));
    }

    @Async
    @EventListener
    public void publishUserHasWorkedOvertimeUpdated(UserHasWorkedOvertimeUpdatedEvent event) {

        tenantContextHolder.getCurrentTenantId().ifPresentOrElse(
            tenantId -> publishRabbitOvertimeUpdated(event, tenantId),
            () -> LOG.error("Cannot publish rabbit overtimeUpdated event for date={} user={} without tenantId.", event.date(), event.userIdComposite())
        );
    }

    private void publishRabbitOvertime(UserHasWorkedOvertimeEvent event, TenantId tenantId) {

        final UserIdComposite userIdComposite = event.userIdComposite();
        final OvertimeHours overtimeHours = event.overtimeHours();

        final OvertimeRabbitEvent overtimeRabbitEvent = new OvertimeRabbitEvent(
            UUID.randomUUID(),
            tenantId.tenantId(),
            userIdComposite.id().value(),
            event.date(),
            overtimeHours.durationInMinutes()
        );

        final String topic = overtimeRabbitmqConfigurationProperties.getTopic();
        final String routingKey = overtimeRabbitmqConfigurationProperties.getRoutingKeyEntered().formatted(tenantId.tenantId());

        LOG.info("publish rabbit OvertimeEvent id={} tenantId={} user={} date={} to topic={}", overtimeRabbitEvent.id(), tenantId.tenantId(), event.userIdComposite(), event.date(), topic);
        rabbitTemplate.convertAndSend(topic, routingKey, overtimeRabbitEvent);
    }

    private void publishRabbitOvertimeUpdated(UserHasWorkedOvertimeUpdatedEvent event, TenantId tenantId) {

        final UserIdComposite userIdComposite = event.userIdComposite();
        final OvertimeHours overtimeHours = event.overtimeHours();

        final OvertimeUpdatedRabbitEvent overtimeUpdatedRabbitEvent = new OvertimeUpdatedRabbitEvent(
            UUID.randomUUID(),
            tenantId.tenantId(),
            userIdComposite.id().value(),
            event.date(),
            overtimeHours.durationInMinutes()
        );

        final String topic = overtimeRabbitmqConfigurationProperties.getTopic();
        final String routingKey = overtimeRabbitmqConfigurationProperties.getRoutingKeyUpdated().formatted(tenantId.tenantId());

        LOG.info("publish rabbit OvertimeUpdatedEvent id={} tenantId={} user={} date={} to topic={}", overtimeUpdatedRabbitEvent.id(), tenantId.tenantId(), event.userIdComposite(), event.date(), topic);
        rabbitTemplate.convertAndSend(topic, routingKey, overtimeUpdatedRabbitEvent);
    }
}
