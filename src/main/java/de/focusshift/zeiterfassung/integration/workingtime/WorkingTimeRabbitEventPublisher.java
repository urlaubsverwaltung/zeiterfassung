package de.focusshift.zeiterfassung.integration.workingtime;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCreatedEvent;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeDeletedEvent;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeUpdatedEvent;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.EnumMap;
import java.util.UUID;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

class WorkingTimeRabbitEventPublisher {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final RabbitTemplate rabbitTemplate;
    private final TenantContextHolder tenantContextHolder;
    private final WorkingTimeRabbitmqConfigurationProperties properties;

    WorkingTimeRabbitEventPublisher(
        RabbitTemplate rabbitTemplate,
        TenantContextHolder tenantContextHolder,
        WorkingTimeRabbitmqConfigurationProperties properties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.tenantContextHolder = tenantContextHolder;
        this.properties = properties;
    }

    @EventListener
    public void publishWorkingTimeCreated(WorkingTimeCreatedEvent event) {
        final UserIdComposite userIdComposite = event.userIdComposite();
        final EnumMap<DayOfWeek, Duration> workdays = event.workdays();

        tenantContextHolder.getCurrentTenantId().ifPresentOrElse(
            tenantId -> {
                final WorkingTimeCreatedRabbitEvent rabbitEvent = new WorkingTimeCreatedRabbitEvent(
                    UUID.randomUUID(),
                    tenantId.tenantId(),
                    userIdComposite.id().value(),
                    event.validFrom(),
                    event.federalState().name(),
                    event.worksOnPublicHoliday(),
                    workdays.getOrDefault(DayOfWeek.MONDAY, Duration.ZERO),
                    workdays.getOrDefault(DayOfWeek.TUESDAY, Duration.ZERO),
                    workdays.getOrDefault(DayOfWeek.WEDNESDAY, Duration.ZERO),
                    workdays.getOrDefault(DayOfWeek.THURSDAY, Duration.ZERO),
                    workdays.getOrDefault(DayOfWeek.FRIDAY, Duration.ZERO),
                    workdays.getOrDefault(DayOfWeek.SATURDAY, Duration.ZERO),
                    workdays.getOrDefault(DayOfWeek.SUNDAY, Duration.ZERO)
                );

                final String topic = properties.topic();
                final String routingKey = properties.routingKeyCreated().formatted(tenantId.tenantId());

                LOG.info("publish rabbit WorkingTimeCreatedEvent id={} tenantId={} user={} to topic={}", rabbitEvent.id(), tenantId.tenantId(), userIdComposite, topic);
                rabbitTemplate.convertAndSend(topic, routingKey, rabbitEvent);
            },
            () -> LOG.error("Cannot publish working time created event for user={} without tenantId.", userIdComposite)
        );
    }

    @EventListener
    public void publishWorkingTimeUpdated(WorkingTimeUpdatedEvent event) {
        final UserIdComposite userIdComposite = event.userIdComposite();
        final EnumMap<DayOfWeek, Duration> workdays = event.workdays();

        tenantContextHolder.getCurrentTenantId().ifPresentOrElse(
            tenantId -> {
                final WorkingTimeUpdatedRabbitEvent rabbitEvent = new WorkingTimeUpdatedRabbitEvent(
                    UUID.randomUUID(),
                    tenantId.tenantId(),
                    userIdComposite.id().value(),
                    event.validFrom(),
                    event.federalState().name(),
                    event.worksOnPublicHoliday(),
                    workdays.getOrDefault(DayOfWeek.MONDAY, Duration.ZERO),
                    workdays.getOrDefault(DayOfWeek.TUESDAY, Duration.ZERO),
                    workdays.getOrDefault(DayOfWeek.WEDNESDAY, Duration.ZERO),
                    workdays.getOrDefault(DayOfWeek.THURSDAY, Duration.ZERO),
                    workdays.getOrDefault(DayOfWeek.FRIDAY, Duration.ZERO),
                    workdays.getOrDefault(DayOfWeek.SATURDAY, Duration.ZERO),
                    workdays.getOrDefault(DayOfWeek.SUNDAY, Duration.ZERO)
                );

                final String topic = properties.topic();
                final String routingKey = properties.routingKeyUpdated().formatted(tenantId.tenantId());

                LOG.info("publish rabbit WorkingTimeUpdatedEvent id={} tenantId={} user={} to topic={}", rabbitEvent.id(), tenantId.tenantId(), userIdComposite, topic);
                rabbitTemplate.convertAndSend(topic, routingKey, rabbitEvent);
            },
            () -> LOG.error("Cannot publish working time updated event for user={} without tenantId.", userIdComposite)
        );
    }

    @EventListener
    public void publishWorkingTimeDeleted(WorkingTimeDeletedEvent event) {
        final UserIdComposite userIdComposite = event.userIdComposite();

        tenantContextHolder.getCurrentTenantId().ifPresentOrElse(
            tenantId -> {
                final WorkingTimeDeletedRabbitEvent rabbitEvent = new WorkingTimeDeletedRabbitEvent(
                    UUID.randomUUID(),
                    tenantId.tenantId(),
                    userIdComposite.id().value(),
                    event.validFrom()
                );

                final String topic = properties.topic();
                final String routingKey = properties.routingKeyDeleted().formatted(tenantId.tenantId());

                LOG.info("publish rabbit WorkingTimeDeletedEvent id={} tenantId={} user={} to topic={}", rabbitEvent.id(), tenantId.tenantId(), userIdComposite, topic);
                rabbitTemplate.convertAndSend(topic, routingKey, rabbitEvent);
            },
            () -> LOG.error("Cannot publish working time deleted event for user={} without tenantId.", userIdComposite)
        );
    }
}
