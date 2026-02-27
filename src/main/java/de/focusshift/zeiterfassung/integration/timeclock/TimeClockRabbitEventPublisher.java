package de.focusshift.zeiterfassung.integration.timeclock;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.timeclock.TimeClockStartedEvent;
import de.focusshift.zeiterfassung.timeclock.TimeClockStoppedEvent;
import de.focusshift.zeiterfassung.timeclock.TimeClockUpdatedEvent;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;

import java.util.UUID;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

class TimeClockRabbitEventPublisher {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final RabbitTemplate rabbitTemplate;
    private final TenantContextHolder tenantContextHolder;
    private final TimeClockRabbitmqConfigurationProperties properties;

    TimeClockRabbitEventPublisher(
        RabbitTemplate rabbitTemplate,
        TenantContextHolder tenantContextHolder,
        TimeClockRabbitmqConfigurationProperties properties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.tenantContextHolder = tenantContextHolder;
        this.properties = properties;
    }

    @EventListener
    public void publishTimeClockStarted(TimeClockStartedEvent event) {
        tenantContextHolder.getCurrentTenantId().ifPresentOrElse(
            tenantId -> {
                final TimeClockStartedRabbitEvent rabbitEvent = new TimeClockStartedRabbitEvent(
                    UUID.randomUUID(),
                    tenantId.tenantId(),
                    event.userId().value(),
                    event.startedAt().toInstant(),
                    event.comment(),
                    event.isBreak()
                );

                final String topic = properties.topic();
                final String routingKey = properties.routingKeyStarted().formatted(tenantId.tenantId());

                LOG.info("publish rabbit TimeClockStartedEvent id={} tenantId={} user={} to topic={}", rabbitEvent.id(), tenantId.tenantId(), event.userId(), topic);
                rabbitTemplate.convertAndSend(topic, routingKey, rabbitEvent);
            },
            () -> LOG.error("Cannot publish time clock started event for user={} without tenantId.", event.userId())
        );
    }

    @EventListener
    public void publishTimeClockUpdated(TimeClockUpdatedEvent event) {
        tenantContextHolder.getCurrentTenantId().ifPresentOrElse(
            tenantId -> {
                final TimeClockUpdatedRabbitEvent rabbitEvent = new TimeClockUpdatedRabbitEvent(
                    UUID.randomUUID(),
                    tenantId.tenantId(),
                    event.userId().value(),
                    event.startedAt().toInstant(),
                    event.comment(),
                    event.isBreak()
                );

                final String topic = properties.topic();
                final String routingKey = properties.routingKeyUpdated().formatted(tenantId.tenantId());

                LOG.info("publish rabbit TimeClockUpdatedEvent id={} tenantId={} user={} to topic={}", rabbitEvent.id(), tenantId.tenantId(), event.userId(), topic);
                rabbitTemplate.convertAndSend(topic, routingKey, rabbitEvent);
            },
            () -> LOG.error("Cannot publish time clock updated event for user={} without tenantId.", event.userId())
        );
    }

    @EventListener
    public void publishTimeClockStopped(TimeClockStoppedEvent event) {
        tenantContextHolder.getCurrentTenantId().ifPresentOrElse(
            tenantId -> {
                final TimeClockStoppedRabbitEvent rabbitEvent = new TimeClockStoppedRabbitEvent(
                    UUID.randomUUID(),
                    tenantId.tenantId(),
                    event.userId().value(),
                    event.startedAt().toInstant(),
                    event.stoppedAt().toInstant(),
                    event.comment(),
                    event.isBreak()
                );

                final String topic = properties.topic();
                final String routingKey = properties.routingKeyStopped().formatted(tenantId.tenantId());

                LOG.info("publish rabbit TimeClockStoppedEvent id={} tenantId={} user={} to topic={}", rabbitEvent.id(), tenantId.tenantId(), event.userId(), topic);
                rabbitTemplate.convertAndSend(topic, routingKey, rabbitEvent);
            },
            () -> LOG.error("Cannot publish time clock stopped event for user={} without tenantId.", event.userId())
        );
    }
}
