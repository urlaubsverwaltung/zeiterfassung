package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.timeentry.events.DayLockedEvent;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

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
    public void checkDayLockedAndPublish() {
        // using Europe/Berlin because this is also the hard coded value in UserSettingProvider currently.
        // and our application is used in germany.
        final ZoneId zoneId = ZoneId.of("Europe/Berlin");

        LOG.info("Check whether a day has to be locked or not. Using zoneId={}", zoneId);

        // actually we have to do this for very person
        // however, every person has zoneId Europe/Berlin currently
        timeEntryLockService.getMinValidTimeEntryDate(zoneId)
            .ifPresentOrElse(
            date -> {
                    final LocalDate lockedDate = date.minusDays(1);
                    LOG.info("Date {} is locked now for zoneId={}. Publish application event.", zoneId, lockedDate);
                    applicationEventPublisher.publishEvent(new DayLockedEvent(lockedDate, zoneId));
                },
                () -> LOG.info("No date to lock available.")
            );
    }
}
