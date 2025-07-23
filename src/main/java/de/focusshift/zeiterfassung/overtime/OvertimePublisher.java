package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.overtime.events.UserHasWorkedOvertimeEvent;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.timeentry.events.DayLockedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryCreatedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryDeletedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryUpdatedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryUpdatedEvent.UpdatedValueCandidate;
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
        LOG.info("Handling DayLockedEvent for date={} and zoneId={}. Calculating overtime and publish application event.", event.date(), event.zoneId());

        final Map<UserIdComposite, OvertimeAccount> overtimeAccountByUserId = overtimeAccountService.getAllOvertimeAccounts();

        overtimeService.getOvertimeForDate(event.date()).forEach((userIdComposite, overtimeHours) -> {

            // currently we just know whether overtime is allowed or not.
            // this may change in the future to have a date range for this info or more granular stuff.
            final OvertimeAccount overtimeAccount = overtimeAccountByUserId.get(userIdComposite);
            if (overtimeAccount.isAllowed() && !overtimeHours.durationInMinutes().isZero()) {
                final UserHasWorkedOvertimeEvent overtimeEvent = new UserHasWorkedOvertimeEvent(userIdComposite, event.date(), overtimeHours);
                LOG.info("publish UserHasWorkedOvertimeEvent date={} user={}", event.date(), userIdComposite);
                applicationEventPublisher.publishEvent(overtimeEvent);
            }
        });
    }

    @EventListener
    public void publishOvertimeUpdated(TimeEntryCreatedEvent event) {

        if (!event.locked()) {
            LOG.debug("Ignore not locked TimeEntryCreatedEvent.");
            return;
        }

        if (event.workDuration().durationInMinutes().isZero()) {
            LOG.info("Ignore TimeEntryCreatedEvent. WorkDuration is zero.");
            return;
        }

        final UserIdComposite userIdComposite = event.ownerUserIdComposite();
        final UserLocalId userLocalId = userIdComposite.localId();
        final OvertimeAccount overtimeAccount = overtimeAccountService.getOvertimeAccount(userLocalId);
        if (!overtimeAccount.isAllowed()) {
            LOG.info("Ignore TimeEntryCreatedEvent. User {} overtime not allowed.", userIdComposite);
            return;
        }

        LOG.info("TimeEntry created and locked.");
        publishUpdated(userIdComposite, event.date());
    }

    @EventListener
    public void publishOvertimeUpdated(TimeEntryUpdatedEvent event) {
        // attributes to consider
        // - locked
        //     not locked timeEntries must not be published since they will be handled by the scheduler in the future
        // - date
        //     an updated date results in updated overtime hours for the previous and the new date! check for locked for the date!
        // - workDuration
        //     at the same locked date, overtime hours can only change when workDuration changed
        //     (e.g. the comment could be updated by someone. however, that doesn't change the overtime hours)
        //
        //
        // | prev locked | current locked | lock changed | date changed | workDuration changed | publish previous date | publish current date |
        // | ----------- | -------------- | ------------ | ------------ | -------------------- | --------------------- | -------------------- |
        // | no          | no             | no           | -            | -                    | -                     | -                    |
        // | no          | yes            | yes          | yes          | -                    | -                     | yes                  |
        // | yes         | no             | yes          | yes          | -                    | yes                   | -                    |
        // | yes         | yes            | no           | yes          | -                    | yes                   | yes                  |
        // | yes         | yes            | no           | no           | no                   | -                     | -                    |
        // | yes         | yes            | no           | no           | yes                  | -                     | yes                  |
        //
        final UpdatedValueCandidate<Boolean> lockedCandidate = event.lockedCandidate();
        final UpdatedValueCandidate<LocalDate> dateCandidate = event.dateCandidate();
        final UpdatedValueCandidate<WorkDuration> workDurationCandidate = event.workDurationCandidate();

        final boolean preLocked = lockedCandidate.previous();
        final boolean curLocked = lockedCandidate.current();
        final boolean dateChanged = dateCandidate.hasChanged();
        final boolean workDurationChanged = workDurationCandidate.hasChanged();

        if (!preLocked && !curLocked) {
            LOG.debug("Ignore not locked TimeEntryUpdate.");
            return;
        }

        final UserIdComposite userIdComposite = event.ownerUserIdComposite();
        final UserLocalId userLocalId = userIdComposite.localId();
        final OvertimeAccount overtimeAccount = overtimeAccountService.getOvertimeAccount(userLocalId);
        if (!overtimeAccount.isAllowed()) {
            LOG.info("Ignore TimeEntryUpdatedEvent. User {} overtime not allowed.", userIdComposite);
            return;
        }

        if (preLocked && dateChanged) {
            LOG.info("TimeEntry date changed and was previously locked.");
            publishUpdated(userIdComposite, dateCandidate.previous());
        }

        if (!preLocked && dateChanged) {
            LOG.info("TimeEntry date changed and is now locked.");
            publishUpdated(userIdComposite, dateCandidate.current());
        } else if (curLocked) {
            if (dateChanged) {
                LOG.info("TimeEntry date changed and is still locked.");
                publishUpdated(userIdComposite, dateCandidate.current());
            } else if (workDurationChanged) {
                LOG.info("TimeEntry workDuration changed and it still locked.");
                publishUpdated(userIdComposite, dateCandidate.current());
            }
        }
    }

    @EventListener
    public void publishOvertimeUpdated(TimeEntryDeletedEvent event) {

        final boolean locked = event.locked();
        final LocalDate date = event.date();
        final WorkDuration workDuration = event.workDuration();

        if (!locked) {
            LOG.info("Ignore not locked TimeEntryDeleteEvent.");
            return;
        }

        final UserIdComposite userIdComposite = event.ownerUserIdComposite();
        final UserLocalId userLocalId = userIdComposite.localId();

        final OvertimeAccount overtimeAccount = overtimeAccountService.getOvertimeAccount(userLocalId);
        // currently we do not have time spans for overtime-allowed/not-allowed
        // otherwise we would have to check the date here!
        if (!overtimeAccount.isAllowed()) {
            LOG.info("Ignore TimeEntryDeletedEvent. User {} overtime not allowed.", userIdComposite);
            return;
        }

        if (workDuration.durationInMinutes().isZero()) {
            LOG.info("Ignore TimeEntryDeleted Event. WorkDuration is zero.");
            return;
        }

        publishUpdated(userIdComposite, date);
    }

    private void publishUpdated(UserIdComposite userIdComposite, LocalDate date) {

        final OvertimeHours overtimeHours = overtimeService.getOvertimeForDateAndUser(date, userIdComposite.localId());

        final UserHasWorkedOvertimeEvent overtimeUpdatedEvent = new UserHasWorkedOvertimeEvent(userIdComposite, date, overtimeHours);
        LOG.info("publish UserHasWorkedOvertimeEvent date={} user={}", overtimeUpdatedEvent, userIdComposite);
        applicationEventPublisher.publishEvent(overtimeUpdatedEvent);
    }
}
