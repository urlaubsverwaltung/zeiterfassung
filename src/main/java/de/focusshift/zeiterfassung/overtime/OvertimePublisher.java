package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.timeentry.DayLockedEvent;
import de.focusshift.zeiterfassung.timeentry.TimeEntryUpdatedEvent;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccount;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccountService;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.slf4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Component
class OvertimePublisher {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final OvertimeService overtimeService;
    private final OvertimeAccountService overtimeAccountService;
    private final ApplicationEventPublisher applicationEventPublisher;

    OvertimePublisher(
        OvertimeService overtimeService,
        OvertimeAccountService overtimeAccountService,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.overtimeService = overtimeService;
        this.overtimeAccountService = overtimeAccountService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener
    public void publishOvertime(DayLockedEvent event) {
        LOG.info("TimeEntry Locking enabled -> fetch timeEntries and publish overtime entries.");

        final Map<UserIdComposite, OvertimeAccount> overtimeAccountByUserId = overtimeAccountService.getAllOvertimeAccounts();

        overtimeService.getOvertimeForDate(event.date()).forEach((userIdComposite, overtimeHours) -> {

            // currently we just know whether overtime is allowed or not.
            // this will change in the future to know from $date to $date and maybe more granular stuff.
            // for the moment: publish hasMadeOvertime? yes or no.
            final OvertimeAccount overtimeAccount = overtimeAccountByUserId.get(userIdComposite);
            if (overtimeAccount.isAllowed() && !overtimeHours.durationInMinutes().isZero()) {
                final UserHasMadeOvertimeEvent overtimeEvent = new UserHasMadeOvertimeEvent(userIdComposite, event.date(), overtimeHours);
                LOG.info("publish UserHasMadeOvertimeEvent date={} user={}", event.date(), userIdComposite);
                applicationEventPublisher.publishEvent(overtimeEvent);
            }
        });
    }

    @EventListener
    public void publishUpdatedOvertime(TimeEntryUpdatedEvent event) {

        if (!event.locked()) {
            LOG.info("Updated timeEntry is not locked yet. Ignore TimeEntryUpdatedEvent.");
            return;
        }

        final UserIdComposite userIdComposite = event.ownerUserIdComposite();
        final UserLocalId userLocalId = userIdComposite.localId();
        final OvertimeAccount overtimeAccount = overtimeAccountService.getOvertimeAccount(userLocalId);

        if (!overtimeAccount.isAllowed()) {
            LOG.info("Ignore TimeEntryUpdatedEvent. User {} overtime not allowed.", userIdComposite);
            return;
        }

        final LocalDate date = event.newStartDate().toLocalDate();
        final OvertimeHours overtimeHours = overtimeService.getOvertimeForDateAndUser(date, userLocalId);

        final UserHasUpdatedOvertimeEvent overtimeUpdatedEvent = new UserHasUpdatedOvertimeEvent(userIdComposite, date, overtimeHours);
        LOG.info("publish UserHasUpdatedOvertimeEvent date={} user={}", overtimeUpdatedEvent, userIdComposite);
        applicationEventPublisher.publishEvent(overtimeUpdatedEvent);
    }
}
