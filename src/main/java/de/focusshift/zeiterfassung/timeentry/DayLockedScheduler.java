package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextRunner;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Component
class DayLockedScheduler {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final TenantContextRunner tenantContextRunner;
    private final TimeEntryLockService timeEntryLockService;
    private final ApplicationEventPublisher applicationEventPublisher;

    DayLockedScheduler(
        TenantContextRunner tenantContextRunner,
        TimeEntryLockService timeEntryLockService,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.tenantContextRunner = tenantContextRunner;
        this.timeEntryLockService = timeEntryLockService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "checkOvertimePublish")
    void scheduledCheckOvertimePublish() {
        tenantContextRunner.runForEachActiveTenant(this::checkLockedAndPublishOvertimeForTenant).run();
    }

    private void checkLockedAndPublishOvertimeForTenant() {
        LOG.info("Check whether a day has to be locked or not.");
        timeEntryLockService.getMinValidTimeEntryDate().ifPresentOrElse(
            (date) -> {
                final LocalDate lockedDate = date.minusDays(1);
                LOG.info("Date {} is locked now. Publish application event.", lockedDate);
                applicationEventPublisher.publishEvent(new DayLockedEvent(lockedDate));
            },
            () -> LOG.info("No date to lock available.")
        );
    }
}
