package de.focusshift.zeiterfassung.integration.timeentry;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryCreatedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryDeletedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryUpdatedEvent;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;

import java.time.LocalDate;
import java.util.UUID;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

class TimeEntryRabbitEventPublisher {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final RabbitTemplate rabbitTemplate;
    private final TenantContextHolder tenantContextHolder;
    private final TimeEntryRabbitmqConfigurationProperties properties;

    TimeEntryRabbitEventPublisher(
        RabbitTemplate rabbitTemplate,
        TenantContextHolder tenantContextHolder,
        TimeEntryRabbitmqConfigurationProperties properties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.tenantContextHolder = tenantContextHolder;
        this.properties = properties;
    }

    @EventListener
    public void publishTimeEntryCreated(TimeEntryCreatedEvent event) {
        tenantContextHolder.getCurrentTenantId().ifPresentOrElse(
            tenantId -> {
                final TimeEntryCreatedRabbitEvent rabbitEvent = new TimeEntryCreatedRabbitEvent(
                    UUID.randomUUID(),
                    tenantId.tenantId(),
                    event.ownerUserIdComposite().id().value(),
                    event.date(),
                    event.locked(),
                    event.workDuration().duration()
                );

                final String topic = properties.topic();
                final String routingKey = properties.routingKeyCreated().formatted(tenantId.tenantId());

                LOG.info("publish rabbit TimeEntryCreatedEvent id={} tenantId={} user={} date={} to topic={}", rabbitEvent.id(), tenantId.tenantId(), event.ownerUserIdComposite(), event.date(), topic);
                rabbitTemplate.convertAndSend(topic, routingKey, rabbitEvent);
            },
            () -> LOG.error("Cannot publish time entry created event for date={} user={} without tenantId.", event.date(), event.ownerUserIdComposite())
        );
    }

    @EventListener
    public void publishTimeEntryUpdated(TimeEntryUpdatedEvent event) {
        final LocalDate date = event.dateCandidate().current();
        final UserIdComposite userIdComposite = event.ownerUserIdComposite();

        tenantContextHolder.getCurrentTenantId().ifPresentOrElse(
            tenantId -> {
                final TimeEntryUpdatedRabbitEvent rabbitEvent = new TimeEntryUpdatedRabbitEvent(
                    UUID.randomUUID(),
                    tenantId.tenantId(),
                    userIdComposite.id().value(),
                    date,
                    event.lockedCandidate().current(),
                    event.workDurationCandidate().current().duration()
                );

                final String topic = properties.topic();
                final String routingKey = properties.routingKeyUpdated().formatted(tenantId.tenantId());

                LOG.info("publish rabbit TimeEntryUpdatedEvent id={} tenantId={} user={} date={} to topic={}", rabbitEvent.id(), tenantId.tenantId(), userIdComposite, date, topic);
                rabbitTemplate.convertAndSend(topic, routingKey, rabbitEvent);
            },
            () -> LOG.error("Cannot publish time entry updated event for date={} user={} without tenantId.", date, userIdComposite)
        );
    }

    @EventListener
    public void publishTimeEntryDeleted(TimeEntryDeletedEvent event) {
        tenantContextHolder.getCurrentTenantId().ifPresentOrElse(
            tenantId -> {
                final TimeEntryDeletedRabbitEvent rabbitEvent = new TimeEntryDeletedRabbitEvent(
                    UUID.randomUUID(),
                    tenantId.tenantId(),
                    event.ownerUserIdComposite().id().value(),
                    event.date(),
                    event.locked(),
                    event.workDuration().duration()
                );

                final String topic = properties.topic();
                final String routingKey = properties.routingKeyDeleted().formatted(tenantId.tenantId());

                LOG.info("publish rabbit TimeEntryDeletedEvent id={} tenantId={} user={} date={} to topic={}", rabbitEvent.id(), tenantId.tenantId(), event.ownerUserIdComposite(), event.date(), topic);
                rabbitTemplate.convertAndSend(topic, routingKey, rabbitEvent);
            },
            () -> LOG.error("Cannot publish time entry deleted event for date={} user={} without tenantId.", event.date(), event.ownerUserIdComposite())
        );
    }
}
