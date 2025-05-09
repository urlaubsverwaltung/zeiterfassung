package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.timeentry.DayLockedEvent;
import org.slf4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Component
class OvertimePublisher {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final OvertimeService overtimeService;
    private final ApplicationEventPublisher applicationEventPublisher;

    OvertimePublisher(OvertimeService overtimeService, ApplicationEventPublisher applicationEventPublisher) {
        this.overtimeService = overtimeService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener
    public void publishOvertime(DayLockedEvent event) {
        LOG.info("TimeEntry Locking enabled -> fetch timeEntries and publish overtime entries.");

        overtimeService.getOvertimeForDate(event.date()).forEach((userIdComposite, overtimeHours) -> {
            final UserHasMadeOvertimeEvent overtimeEvent = new UserHasMadeOvertimeEvent(userIdComposite, event.date(), overtimeHours);
            LOG.debug("publish {}", overtimeEvent);
            applicationEventPublisher.publishEvent(overtimeEvent);
        });
    }
}
