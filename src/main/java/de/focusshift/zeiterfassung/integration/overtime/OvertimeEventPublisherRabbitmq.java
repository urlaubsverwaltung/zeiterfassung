package de.focusshift.zeiterfassung.integration.overtime;

import de.focusshift.zeiterfassung.overtime.OvertimeHours;
import de.focusshift.zeiterfassung.overtime.UserHasMadeOvertimeEvent;
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

class OvertimeEventPublisherRabbitmq {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final RabbitTemplate rabbitTemplate;
    private final TenantContextHolder tenantContextHolder;
    private final OvertimeRabbitmqConfigurationProperties overtimeRabbitmqConfigurationProperties;

    OvertimeEventPublisherRabbitmq(
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
    public void publishUserHasMadeOvertime(UserHasMadeOvertimeEvent event) {

        tenantContextHolder.getCurrentTenantId().ifPresentOrElse(
            tenantId -> publishRabbitOvertime(event, tenantId),
            () -> LOG.error("Cannot publish rabbit event {} without tenantId.", event));
    }

    private void publishRabbitOvertime(UserHasMadeOvertimeEvent event, TenantId tenantId) {

        final UserIdComposite userIdComposite = event.userIdComposite();
        final OvertimeHours overtimeHours = event.overtimeHours();

        final OvertimeEvent overtimeEvent = new OvertimeEvent(
            UUID.randomUUID(),
            tenantId.tenantId(),
            userIdComposite.id().value(),
            event.date(),
            overtimeHours.durationInMinutes()
        );

        final String topic = overtimeRabbitmqConfigurationProperties.getTopic();
        final String routingKey = overtimeRabbitmqConfigurationProperties.getRoutingKeyEntered();

        LOG.info("publish rabbit OvertimeEvent id={}", overtimeEvent.id());
        rabbitTemplate.convertAndSend(topic, routingKey, overtimeEvent);
    }
}
