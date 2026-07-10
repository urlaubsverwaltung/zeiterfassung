package de.focusshift.zeiterfassung.timeentry.republish;

import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.util.StringUtils;

import static de.focusshift.zeiterfassung.timeentry.republish.DayLockedRepublishRabbitmqConfiguration.ZEITERFASSUNG_DAY_LOCKED_REPUBLISH_QUEUE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

class DayLockedRepublishEventHandlerRabbitmq {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final DayLockedRepublishService dayLockedRepublishService;

    DayLockedRepublishEventHandlerRabbitmq(DayLockedRepublishService dayLockedRepublishService) {
        this.dayLockedRepublishService = dayLockedRepublishService;
    }

    @RabbitListener(queues = {ZEITERFASSUNG_DAY_LOCKED_REPUBLISH_QUEUE})
    void on(DayLockedRepublishRequest event) {
        if (StringUtils.hasText(event.tenantId())) {
            LOG.info("Received DayLockedRepublishRequest tenantId={} from={} to={}", event.tenantId(), event.from(), event.to());
            dayLockedRepublishService.republishDayLockedEvents(event.tenantId(), event.from(), event.to());
        } else {
            LOG.info("Received DayLockedRepublishRequest from={} to={}", event.from(), event.to());
            dayLockedRepublishService.republishDayLockedEvents(event.from(), event.to());
        }
    }
}
