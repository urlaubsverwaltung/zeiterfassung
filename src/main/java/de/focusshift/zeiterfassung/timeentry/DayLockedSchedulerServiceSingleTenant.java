package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.timeentry.events.DayLockedEvent;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.SINGLE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Service
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = SINGLE, matchIfMissing = true)
class DayLockedSchedulerServiceSingleTenant implements DayLockedSchedulerService{

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final TimeEntryLockService timeEntryLockService;
    private final ApplicationEventPublisher applicationEventPublisher;

    DayLockedSchedulerServiceSingleTenant(
        TimeEntryLockService timeEntryLockService,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.timeEntryLockService = timeEntryLockService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void checkLockedAndPublishOvertime() {
        LOG.info("Check whether a day has to be locked or not.");
        timeEntryLockService.getMinValidTimeEntryDate().ifPresentOrElse(
            date -> {
                final LocalDate lockedDate = date.minusDays(1);
                LOG.info("Date {} is locked now. Publish application event.", lockedDate);
                applicationEventPublisher.publishEvent(new DayLockedEvent(lockedDate));
            },
            () -> LOG.info("No date to lock available.")
        );
    }
}
