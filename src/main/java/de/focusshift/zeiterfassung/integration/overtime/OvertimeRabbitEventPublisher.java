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

import java.time.LocalDate;
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

    @EventListener
    public void publishUserHasWorkedOvertime(UserHasWorkedOvertimeEvent event) {
        publishRabbitEvent(event.userIdComposite(), event.overtimeHours(), event.date());
    }

    @EventListener
    public void publishUserHasWorkedOvertimeUpdated(UserHasWorkedOvertimeUpdatedEvent event) {
        publishRabbitEvent(event.userIdComposite(), event.overtimeHours(), event.date());
    }


    private void publishRabbitEvent(UserIdComposite userIdComposite, OvertimeHours overtimeHours, LocalDate date) {
        tenantContextHolder.getCurrentTenantId().ifPresentOrElse(
            tenantId -> publishRabbitOvertime(userIdComposite, overtimeHours, date, tenantId),
            () -> LOG.error("Cannot publish overtime event for date={} user={} without tenantId.", date, userIdComposite)
        );
    }

    private void publishRabbitOvertime(UserIdComposite userIdComposite, OvertimeHours overtimeHours, LocalDate date, TenantId tenantId) {

        final OvertimeRabbitEvent overtimeRabbitEvent = new OvertimeRabbitEvent(
            UUID.randomUUID(),
            tenantId.tenantId(),
            userIdComposite.id().value(),
            date,
            overtimeHours.durationInMinutes()
        );

        final String topic = overtimeRabbitmqConfigurationProperties.getTopic();
        final String routingKey = overtimeRabbitmqConfigurationProperties.getRoutingKeyEntered().formatted(tenantId.tenantId());

        LOG.info("publish rabbit OvertimeEvent id={} tenantId={} user={} date={} to topic={}", overtimeRabbitEvent.id(), tenantId.tenantId(), userIdComposite, date, topic);
        rabbitTemplate.convertAndSend(topic, routingKey, overtimeRabbitEvent);
    }
}
