package de.focusshift.zeiterfassung.integration.overtimeaccount;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccountUpdatedEvent;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;

import java.util.UUID;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

class OvertimeAccountRabbitEventPublisher {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final RabbitTemplate rabbitTemplate;
    private final TenantContextHolder tenantContextHolder;
    private final OvertimeAccountRabbitmqConfigurationProperties properties;

    OvertimeAccountRabbitEventPublisher(
        RabbitTemplate rabbitTemplate,
        TenantContextHolder tenantContextHolder,
        OvertimeAccountRabbitmqConfigurationProperties properties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.tenantContextHolder = tenantContextHolder;
        this.properties = properties;
    }

    @EventListener
    public void publishOvertimeAccountUpdated(OvertimeAccountUpdatedEvent event) {
        final UserIdComposite userIdComposite = event.userIdComposite();

        tenantContextHolder.getCurrentTenantId().ifPresentOrElse(
            tenantId -> {
                final OvertimeAccountUpdatedRabbitEvent rabbitEvent = new OvertimeAccountUpdatedRabbitEvent(
                    UUID.randomUUID(),
                    tenantId.tenantId(),
                    userIdComposite.id().value(),
                    event.isOvertimeAllowed(),
                    event.maxAllowedOvertime()
                );

                final String topic = properties.topic();
                final String routingKey = properties.routingKeyUpdated().formatted(tenantId.tenantId());

                LOG.info("publish rabbit OvertimeAccountUpdatedEvent id={} tenantId={} user={} to topic={}", rabbitEvent.id(), tenantId.tenantId(), userIdComposite, topic);
                rabbitTemplate.convertAndSend(topic, routingKey, rabbitEvent);
            },
            () -> LOG.error("Cannot publish overtime account updated event for user={} without tenantId.", userIdComposite)
        );
    }
}
